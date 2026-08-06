package com.benly.question.service;

import com.benly.global.exception.BusinessException;
import com.benly.question.client.ClaudeClient; // 추가
import com.benly.question.client.WhisperClient; // 추가
import com.benly.question.dto.AnswerCreateRequest;
import com.benly.question.dto.AnswerResponse;
import com.benly.question.entity.Answer;
import com.benly.question.entity.AnswerType; // 추가
import com.benly.question.entity.NextActionType;
import com.benly.question.entity.Question;
import com.benly.question.exception.AnswerErrorCode;
import com.benly.session.entity.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnswerServiceTest {

    @Mock private AnswerCommandService answerCommandService;

    // 💡 추가됨: AnswerService 내부에서 직접 호출되는 클라이언트들
    @Mock private ClaudeClient claudeClient;
    @Mock private WhisperClient whisperClient;

    @InjectMocks
    private AnswerService answerService;

    private static final Long USER_ID = 1L;
    private static final Long SESSION_ID = 10L;
    private static final Long QUESTION_ID = 100L;
    private static final String VALID_TRANSCRIPT = "재고 차감 로직에서 동시 요청이 몰리는 문제가 있었습니다";

    // 헬퍼 메서드: 성공 시 리턴할 Answer 객체 모킹
    private Answer mockAnswer() {
        Answer answer = mock(Answer.class);
        Question question = mock(Question.class);
        Session session = mock(Session.class);

        // 기본 정보 모킹
        given(answer.getTranscript()).willReturn(VALID_TRANSCRIPT);
        given(answer.getQuestion()).willReturn(question);
        given(question.getSession()).willReturn(session);
        given(question.getParent()).willReturn(null); // 메인 질문으로 가정

        // 💡 추가됨: AnswerResponse.from() 변환 시 발생하는 NPE 방지
        given(answer.getInputType()).willReturn(AnswerType.TEXT);
        given(answer.getSttStatus()).willReturn("COMPLETED");

        return answer;
    }

    // ===== 성공 =====

    @Test
    @DisplayName("성공 - 텍스트 답변 저장")
    void success() {
        // given
        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, VALID_TRANSCRIPT);
        Answer mockAnswer = mockAnswer();
        AnswerResponse.NextAction mockNextAction = AnswerResponse.NextAction.of(NextActionType.FOLLOW_UP, 200L);

        // CommandService 동작 모킹
        given(answerCommandService.saveTextAnswer(SESSION_ID, USER_ID, request)).willReturn(mockAnswer);
        given(answerCommandService.buildContext(any())).willReturn("context");

        // 💡 추가됨: tryCreateFollowUp 내부에서 호출되는 ClaudeClient 모킹
        given(claudeClient.generateFollowUp(anyString())).willReturn("꼬리질문입니다");

        // 💡 추가됨: Claude 호출 성공 후 실행되는 저장 로직 모킹
        given(answerCommandService.saveFollowUpQuestion(any(), any(), any(Integer.class), anyString()))
                .willReturn(mockNextAction);

        // when
        AnswerResponse response = answerService.submitTextAnswer(SESSION_ID, USER_ID, request);

        // then
        verify(answerCommandService).saveTextAnswer(SESSION_ID, USER_ID, request);
        assertThat(response.answer().transcript()).contains("재고 차감");
        assertThat(response.nextAction().type()).isEqualTo(NextActionType.FOLLOW_UP);
    }

    // ===== 실패 케이스 (AnswerCommandService가 예외를 던지는 상황 모킹) =====
    // ... (이하 실패 테스트 6개는 이전 코드와 동일하게 유지)

    @Test
    @DisplayName("실패 - 질문이 없으면 QUESTION_NOT_FOUND")
    void fail_questionNotFound() {
        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, VALID_TRANSCRIPT);

        given(answerCommandService.saveTextAnswer(SESSION_ID, USER_ID, request))
                .willThrow(new BusinessException(AnswerErrorCode.QUESTION_NOT_FOUND));

        assertThatThrownBy(() -> answerService.submitTextAnswer(SESSION_ID, USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AnswerErrorCode.QUESTION_NOT_FOUND);
    }

    @Test
    @DisplayName("실패 - URL 세션과 질문의 세션이 다르면 QUESTION_SESSION_MISMATCH")
    void fail_sessionMismatch() {
        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, VALID_TRANSCRIPT);
        Long wrongSessionId = 999L;

        given(answerCommandService.saveTextAnswer(wrongSessionId, USER_ID, request))
                .willThrow(new BusinessException(AnswerErrorCode.QUESTION_SESSION_MISMATCH));

        assertThatThrownBy(() -> answerService.submitTextAnswer(wrongSessionId, USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AnswerErrorCode.QUESTION_SESSION_MISMATCH);
    }

    @Test
    @DisplayName("실패 - 남의 세션이면 ANSWER_FORBIDDEN")
    void fail_forbidden() {
        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, VALID_TRANSCRIPT);

        given(answerCommandService.saveTextAnswer(SESSION_ID, USER_ID, request))
                .willThrow(new BusinessException(AnswerErrorCode.ANSWER_FORBIDDEN));

        assertThatThrownBy(() -> answerService.submitTextAnswer(SESSION_ID, USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AnswerErrorCode.ANSWER_FORBIDDEN);
    }

    @Test
    @DisplayName("실패 - IN_PROGRESS가 아니면 SESSION_NOT_IN_PROGRESS")
    void fail_notInProgress() {
        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, VALID_TRANSCRIPT);

        given(answerCommandService.saveTextAnswer(SESSION_ID, USER_ID, request))
                .willThrow(new BusinessException(AnswerErrorCode.SESSION_NOT_IN_PROGRESS));

        assertThatThrownBy(() -> answerService.submitTextAnswer(SESSION_ID, USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AnswerErrorCode.SESSION_NOT_IN_PROGRESS);
    }

    @Test
    @DisplayName("실패 - 이미 답변했으면 ALREADY_ANSWERED")
    void fail_alreadyAnswered() {
        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, VALID_TRANSCRIPT);

        given(answerCommandService.saveTextAnswer(SESSION_ID, USER_ID, request))
                .willThrow(new BusinessException(AnswerErrorCode.ALREADY_ANSWERED));

        assertThatThrownBy(() -> answerService.submitTextAnswer(SESSION_ID, USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AnswerErrorCode.ALREADY_ANSWERED);
    }

    @Test
    @DisplayName("실패 - 답변이 너무 짧으면 ANSWER_TOO_SHORT")
    void fail_tooShort() {
        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, "짧음");

        given(answerCommandService.saveTextAnswer(SESSION_ID, USER_ID, request))
                .willThrow(new BusinessException(AnswerErrorCode.ANSWER_TOO_SHORT));

        assertThatThrownBy(() -> answerService.submitTextAnswer(SESSION_ID, USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AnswerErrorCode.ANSWER_TOO_SHORT);
    }
}