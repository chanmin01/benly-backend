package com.benly.question.service;

import com.benly.global.exception.BusinessException;
import com.benly.question.client.ClaudeClient;
import com.benly.question.dto.AnswerCreateRequest;
import com.benly.question.dto.AnswerResponse;
import com.benly.question.dto.NextActionType;
import com.benly.question.entity.Answer;
import com.benly.question.entity.Question;
import com.benly.question.entity.QuestionSourceType;
import com.benly.question.exception.AnswerErrorCode;
import com.benly.question.repository.AnswerRepository;
import com.benly.question.repository.QuestionRepository;
import com.benly.session.entity.Session;
import com.benly.session.entity.SessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final ClaudeClient claudeClient;

    private static final int MIN_ANSWER_LENGTH = 10;
    private static final int MAX_FOLLOW_UP = 2;

    @Transactional
    public AnswerResponse submitTextAnswer(Long sessionId, Long userId, AnswerCreateRequest request) {

        // 1. 질문조회
        Question question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new BusinessException(AnswerErrorCode.QUESTION_NOT_FOUND));

        Session session = question.getSession();

        // 2. URL의 sessionId와 질문의 세션이 일치하는지
        if (!session.getId().equals(sessionId)) {
            throw new BusinessException(AnswerErrorCode.QUESTION_SESSION_MISMATCH);
        }

        // 3. 소유권 검증
        if (!session.getUser().getId().equals(userId)){
            throw new BusinessException(AnswerErrorCode.ANSWER_FORBIDDEN);
        }

        // 4. 세션 상태 검증 (IN_PROGRESS)
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BusinessException(AnswerErrorCode.SESSION_NOT_IN_PROGRESS);
        }

        // 5. 중복 검증
        if (answerRepository.existsByQuestionId(question.getId())) {
            throw new BusinessException(AnswerErrorCode.ALREADY_ANSWERED);
        }

        // 6. 길이 검증
        if (request.transcript().trim().length() < MIN_ANSWER_LENGTH) {
            throw new BusinessException(AnswerErrorCode.ANSWER_TOO_SHORT);
        }

        // 7. Answer 저장
        Answer answer = Answer.createText(question, request.transcript().trim());
        Answer savedAnswer;
        try{
            // 저장 및 flush (동시성 제어)
            savedAnswer =  answerRepository.saveAndFlush(answer);
        } catch (DataIntegrityViolationException ex){
            throw new BusinessException(AnswerErrorCode.ALREADY_ANSWERED);
        }

        AnswerResponse.NextAction nextAction = decideNextAction(question, session);

        return AnswerResponse.from(savedAnswer, nextAction);
    }

    private AnswerResponse.NextAction decideNextAction(Question answeredQuestion,
                                                       Session session) {
        boolean isMain = (answeredQuestion.getParent() == null);

        if (isMain) {
            // 메인에 답변 → 꼬리1 생성 시도
            return tryCreateFollowUp(answeredQuestion, session, 1);
        }

        // 꼬리에 답변 → 그 메인의 꼬리 개수 확인
        Question mainQuestion = answeredQuestion.getParent();
        int followUpCount = questionRepository.countByParent(mainQuestion);

        if (followUpCount < MAX_FOLLOW_UP) {
            // 꼬리2 생성 시도
            return tryCreateFollowUp(mainQuestion, session, followUpCount + 1);
        }

        // 꼬리 2개 다 함 → 다음 메인 or FINISH
        return decideNextMainOrFinish(mainQuestion, session);
    }

    // 꼬리질문 생성 시도 (실패해도 답변은 유지, 다음 메인으로)
    private AnswerResponse.NextAction tryCreateFollowUp(Question mainQuestion,
                                                        Session session, int followUpSeq) {
        try {
            String context = buildContext(mainQuestion);
            String content = claudeClient.generateFollowUp(mainQuestion.getContent(), context);

            Question followUp = Question.createFollowUp(
                    session, mainQuestion, followUpSeq, content, QuestionSourceType.CLAUDE);
            Question savedFollowUp = questionRepository.save(followUp);

            return AnswerResponse.NextAction.of(NextActionType.FOLLOW_UP, savedFollowUp.getId());
        } catch (Exception e) {
            // 꼬리 생성 실패 → 답변은 이미 저장됨, 다음 메인으로 넘어감
            log.warn("꼬리질문 생성 실패, 다음 메인으로 진행. mainQuestionId={}",
                    mainQuestion.getId(), e);
            return decideNextMainOrFinish(mainQuestion, session);
        }
    }

    // 다음 메인 or 종료
    private AnswerResponse.NextAction decideNextMainOrFinish(Question mainQuestion,
                                                             Session session) {
        Optional<Question> nextMain = questionRepository
                .findFirstBySessionAndParentIsNullAndSeqGreaterThanOrderBySeqAsc(
                        session, mainQuestion.getSeq());

        if (nextMain.isPresent()) {
            return AnswerResponse.NextAction.of(
                    NextActionType.NEXT_MAIN, nextMain.get().getId());
        }

        // 다음 메인 없음 → 면접 종료
        session.markCompleted();
        return AnswerResponse.NextAction.of(NextActionType.FINISH, null);
    }

    // Claude에 줄 맥락 (메인 답변 + 이전 꼬리들 + 답변)
    private String buildContext(Question mainQuestion) {
        StringBuilder sb = new StringBuilder();

        // 메인 답변
        answerRepository.findByQuestionId(mainQuestion.getId())
                .ifPresent(a -> sb.append("메인 질문에 대한 답변: ")
                        .append(a.getTranscript()).append("\n"));

        // 이전 꼬리들 + 답변
        List<Question> followUps = questionRepository.findByParentOrderBySeqAsc(mainQuestion);
        for (Question fu : followUps) {
            sb.append("꼬리질문: ").append(fu.getContent()).append("\n");
            answerRepository.findByQuestionId(fu.getId())
                    .ifPresent(a -> sb.append("그에 대한 답변: ")
                            .append(a.getTranscript()).append("\n"));
        }

        return sb.toString();
    }

}
