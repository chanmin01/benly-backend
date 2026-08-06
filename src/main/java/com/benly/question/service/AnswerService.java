package com.benly.question.service;

import com.benly.question.client.ClaudeClient;
import com.benly.question.client.WhisperClient;
import com.benly.question.dto.AnswerCreateRequest;
import com.benly.question.dto.AnswerResponse;
import com.benly.question.entity.Answer;
import com.benly.question.entity.Question;
import com.benly.session.entity.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
// 클래스 레벨의 @Transactional을 제거하여 외부 API 통신 중 커넥션 점유를 막습니다.
public class AnswerService {

    private final ClaudeClient claudeClient;
    private final WhisperClient whisperClient;
    private final AnswerCommandService answerCommandService; // DB 트랜잭션을 담당할 별도 빈 주입

    private static final int MAX_FOLLOW_UP = 2;

    public AnswerResponse submitTextAnswer(Long sessionId, Long userId, AnswerCreateRequest request) {
        // 1. 답변 DB 저장 (짧은 트랜잭션 수행 후 커밋)
        Answer savedAnswer = answerCommandService.saveTextAnswer(sessionId, userId, request);

        // 2. nextAction (트랜잭션 밖에서 Claude 호출 및 다음 상태 결정)
        AnswerResponse.NextAction nextAction = decideNextAction(savedAnswer.getQuestion(), savedAnswer.getQuestion().getSession());

        return AnswerResponse.from(savedAnswer, nextAction);
    }

    public AnswerResponse submitAudioAnswer(Long sessionId, Long userId, Long questionId, MultipartFile audioFile, Integer durationSec) {
        // 1. 외부 API(Whisper) 호출 전 권한, 상태 등 사전 검증 (비인가자의 리소스 소모 차단)
        answerCommandService.validateBeforeStt(questionId, sessionId, userId);

        // 2. 음성 → 텍스트 (Whisper) - 트랜잭션이 없는 상태에서 긴 시간동안 API 호출 진행
        String transcript = whisperClient.transcribe(audioFile);

        // 3. 저장 (saveAndFlush + 동시성) - 짧은 트랜잭션 수행 후 커밋
        Answer savedAnswer = answerCommandService.saveAudioAnswer(sessionId, userId, questionId, transcript, durationSec);

        // 4. nextAction (트랜잭션 밖에서 Claude 호출 및 다음 상태 결정)
        AnswerResponse.NextAction nextAction = decideNextAction(savedAnswer.getQuestion(), savedAnswer.getQuestion().getSession());

        return AnswerResponse.from(savedAnswer, nextAction);
    }

    private AnswerResponse.NextAction decideNextAction(Question answeredQuestion, Session session) {
        boolean isMain = (answeredQuestion.getParent() == null);

        if (isMain) {
            // 메인에 답변 → 꼬리1 생성 시도
            return tryCreateFollowUp(answeredQuestion, session, 1);
        }

        // 꼬리에 답변 → 그 메인의 꼬리 개수 확인
        Question mainQuestion = answeredQuestion.getParent();
        int followUpCount = answerCommandService.countFollowUps(mainQuestion);

        if (followUpCount < MAX_FOLLOW_UP) {
            // 꼬리2 생성 시도
            return tryCreateFollowUp(mainQuestion, session, followUpCount + 1);
        }

        // 꼬리 2개 다 함 → 다음 메인 or FINISH (엔티티 대신 식별자 ID를 넘김)
        return answerCommandService.decideNextMainOrFinish(mainQuestion.getId(), session.getId());
    }

    // 꼬리질문 생성 시도 (실패해도 답변은 유지, 다음 메인으로)
    private AnswerResponse.NextAction tryCreateFollowUp(Question mainQuestion, Session session, int followUpSeq) {
        try {
            String context = answerCommandService.buildContext(mainQuestion);
            // 외부 트랜잭션 밖에서 수 초 대기하며 Claude API 호출
            String content = claudeClient.generateFollowUp(context);

            // API 호출 성공 시 독립된 새 트랜잭션(REQUIRES_NEW)으로 DB에 꼬리질문 저장
            return answerCommandService.saveFollowUpQuestion(session, mainQuestion, followUpSeq, content);
        } catch (Exception e) {
            // 꼬리 생성 실패 → 메인 답변은 이미 커밋되어 저장됨, 다음 메인으로 넘어감
            log.warn("꼬리질문 생성 실패, 다음 메인으로 진행. mainQuestionId={}", mainQuestion.getId(), e);

            // 엔티티 대신 식별자 ID를 넘김
            return answerCommandService.decideNextMainOrFinish(mainQuestion.getId(), session.getId());
        }
    }
}