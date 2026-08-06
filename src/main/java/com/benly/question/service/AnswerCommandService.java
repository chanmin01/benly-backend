package com.benly.question.service;

import com.benly.global.exception.BusinessException;
import com.benly.question.dto.AnswerCreateRequest;
import com.benly.question.dto.AnswerResponse;
import com.benly.question.entity.Answer;
import com.benly.question.entity.NextActionType;
import com.benly.question.entity.Question;
import com.benly.question.entity.QuestionSourceType;
import com.benly.question.exception.AnswerErrorCode;
import com.benly.question.repository.AnswerRepository;
import com.benly.question.repository.QuestionRepository;
import com.benly.session.entity.Session;
import com.benly.session.entity.SessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AnswerCommandService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    private static final int MIN_ANSWER_LENGTH = 10;

    @Transactional
    public Answer saveTextAnswer(Long sessionId, Long userId, AnswerCreateRequest request) {
        Question question = validateAndGetQuestion(request.questionId(), sessionId, userId);

        // 6. 길이 검증
        if (request.transcript().trim().length() < MIN_ANSWER_LENGTH) {
            throw new BusinessException(AnswerErrorCode.ANSWER_TOO_SHORT);
        }

        // 7. Answer 저장
        Answer answer = Answer.createText(question, request.transcript().trim());
        try {
            // 저장 및 flush (동시성 제어)
            return answerRepository.saveAndFlush(answer);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(AnswerErrorCode.ALREADY_ANSWERED);
        }
    }

    @Transactional
    public Answer saveAudioAnswer(Long sessionId, Long userId, Long questionId, String transcript, Integer durationSec) {
        Question question = validateAndGetQuestion(questionId, sessionId, userId);

        // 7. 길이 검증
        if (transcript.trim().length() < MIN_ANSWER_LENGTH) {
            throw new BusinessException(AnswerErrorCode.ANSWER_TOO_SHORT);
        }

        // 8. 저장 (saveAndFlush + 동시성)
        Answer answer = Answer.createAudio(question, transcript.trim(), durationSec);
        try {
            return answerRepository.saveAndFlush(answer);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(AnswerErrorCode.ALREADY_ANSWERED);
        }
    }

    // 공통 검증 로직 추출
    private Question validateAndGetQuestion(Long questionId, Long sessionId, Long userId) {
        // 1. 질문조회
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(AnswerErrorCode.QUESTION_NOT_FOUND));

        Session session = question.getSession();

        // 2. URL의 sessionId와 질문의 세션이 일치하는지
        if (!session.getId().equals(sessionId)) {
            throw new BusinessException(AnswerErrorCode.QUESTION_SESSION_MISMATCH);
        }
        // 3. 소유권 검증
        if (!session.getUser().getId().equals(userId)) {
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

        return question;
    }

    @Transactional(readOnly = true)
    public int countFollowUps(Question mainQuestion) {
        return questionRepository.countByParent(mainQuestion);
    }

    @Transactional(readOnly = true)
    public String buildContext(Question mainQuestion) {
        StringBuilder sb = new StringBuilder();

        // 메인 질문 + 답변
        sb.append("메인 질문: ").append(mainQuestion.getContent()).append("\n");
        answerRepository.findByQuestionId(mainQuestion.getId())
                .ifPresent(a -> sb.append("답변: ").append(a.getTranscript()).append("\n"));

        // 꼬리질문들 + 답변
        List<Question> followUps = questionRepository.findByParentOrderBySeqAsc(mainQuestion);
        for (Question fu : followUps) {
            sb.append("꼬리질문: ").append(fu.getContent()).append("\n");
            answerRepository.findByQuestionId(fu.getId())
                    .ifPresent(a -> sb.append("답변: ").append(a.getTranscript()).append("\n"));
        }

        return sb.toString();
    }

    // 꼬리질문 생성 시 에러가 나도 기존 답변 롤백을 방지하기 위해 완전히 분리된 트랜잭션 사용 (REQUIRES_NEW)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AnswerResponse.NextAction saveFollowUpQuestion(Session session, Question mainQuestion, int followUpSeq, String content) {
        Question followUp = Question.createFollowUp(
                session, mainQuestion, followUpSeq, content, QuestionSourceType.CLAUDE);
        Question savedFollowUp = questionRepository.save(followUp);

        return AnswerResponse.NextAction.of(NextActionType.FOLLOW_UP, savedFollowUp.getId());
    }

    // 다음 메인 or 종료 판단 및 상태 변경도 독립 트랜잭션 사용
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AnswerResponse.NextAction decideNextMainOrFinish(Question mainQuestion, Session session) {
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
}