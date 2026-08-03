package com.benly.question.service;

import com.benly.global.exception.BusinessException;
import com.benly.question.dto.AnswerCreateRequest;
import com.benly.question.dto.AnswerResponse;
import com.benly.question.entity.Answer;
import com.benly.question.entity.Question;
import com.benly.question.exception.AnswerErrorCode;
import com.benly.question.repository.AnswerRepository;
import com.benly.question.repository.QuestionRepository;
import com.benly.session.entity.Session;
import com.benly.session.entity.SessionStatus;
import com.benly.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // 불필요한 스터빙 에러(UnnecessaryStubbing) 방지
class AnswerServiceTest {

    @Mock private AnswerRepository answerRepository;
    @Mock private QuestionRepository questionRepository;

    @InjectMocks
    private AnswerService answerService;

    private static final Long USER_ID = 1L;
    private static final Long SESSION_ID = 10L;
    private static final Long QUESTION_ID = 100L;
    private static final String VALID_TRANSCRIPT = "재고 차감 로직에서 동시 요청이 몰리는 문제가 있었습니다";

    // 정상 상태의 Mock들을 만드는 헬퍼 (각 테스트에서 필요한 것만 덮어씀)
    private User mockUser(Long id) {
        User user = mock(User.class);
        given(user.getId()).willReturn(id);
        return user;
    }

    private Session mockSession(User owner, SessionStatus status) {
        Session session = mock(Session.class);
        given(session.getId()).willReturn(SESSION_ID);
        given(session.getUser()).willReturn(owner);
        given(session.getStatus()).willReturn(status);
        return session;
    }

    private Question mockQuestion(Session session) {
        Question question = mock(Question.class);
        given(question.getId()).willReturn(QUESTION_ID);
        given(question.getSession()).willReturn(session);
        return question;
    }

    // ===== 성공 =====

    @Test
    @DisplayName("성공 - 텍스트 답변 저장")
    void success() {
        User user = mockUser(USER_ID);
        Session session = mockSession(user, SessionStatus.IN_PROGRESS);
        Question question = mockQuestion(session);

        given(questionRepository.findById(QUESTION_ID)).willReturn(Optional.of(question));
        given(answerRepository.existsByQuestionId(QUESTION_ID)).willReturn(false);
        given(answerRepository.save(any(Answer.class))).willAnswer(inv -> inv.getArgument(0));

        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, VALID_TRANSCRIPT);

        // 수정: 파라미터 순서를 SESSION_ID, USER_ID 순으로 변경
        AnswerResponse response = answerService.submitTextAnswer(SESSION_ID, USER_ID, request);

        verify(answerRepository).save(any(Answer.class));
        assertThat(response.answer().transcript()).contains("재고 차감");
    }

    // ===== 실패 1: 질문 없음 =====

    @Test
    @DisplayName("실패 - 질문이 없으면 QUESTION_NOT_FOUND")
    void fail_questionNotFound() {
        given(questionRepository.findById(QUESTION_ID)).willReturn(Optional.empty());

        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, VALID_TRANSCRIPT);

        // 수정: 파라미터 순서 변경
        assertThatThrownBy(() -> answerService.submitTextAnswer(SESSION_ID, USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AnswerErrorCode.QUESTION_NOT_FOUND);
    }

    // ===== 실패 2: 세션 불일치 =====

    @Test
    @DisplayName("실패 - URL 세션과 질문의 세션이 다르면 QUESTION_SESSION_MISMATCH")
    void fail_sessionMismatch() {
        User user = mockUser(USER_ID);
        Session session = mockSession(user, SessionStatus.IN_PROGRESS);
        Question question = mockQuestion(session);

        given(questionRepository.findById(QUESTION_ID)).willReturn(Optional.of(question));

        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, VALID_TRANSCRIPT);
        Long wrongSessionId = 999L;   // URL 세션이 질문의 세션(10)과 다름

        // 수정: 파라미터 순서 변경 (wrongSessionId가 먼저 옴)
        assertThatThrownBy(() -> answerService.submitTextAnswer(wrongSessionId, USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AnswerErrorCode.QUESTION_SESSION_MISMATCH);
    }

    // ===== 실패 3: 남의 세션 =====

    @Test
    @DisplayName("실패 - 남의 세션이면 ANSWER_FORBIDDEN")
    void fail_forbidden() {
        User otherUser = mockUser(2L);   // 다른 사람
        Session session = mockSession(otherUser, SessionStatus.IN_PROGRESS);
        Question question = mockQuestion(session);

        given(questionRepository.findById(QUESTION_ID)).willReturn(Optional.of(question));

        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, VALID_TRANSCRIPT);

        // 수정: 파라미터 순서 변경
        assertThatThrownBy(() -> answerService.submitTextAnswer(SESSION_ID, USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AnswerErrorCode.ANSWER_FORBIDDEN);
    }

    // ===== 실패 4: 세션 상태 =====

    @Test
    @DisplayName("실패 - IN_PROGRESS가 아니면 SESSION_NOT_IN_PROGRESS")
    void fail_notInProgress() {
        User user = mockUser(USER_ID);
        Session session = mockSession(user, SessionStatus.READY);   // 아직 시작 안 함
        Question question = mockQuestion(session);

        given(questionRepository.findById(QUESTION_ID)).willReturn(Optional.of(question));

        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, VALID_TRANSCRIPT);

        // 수정: 파라미터 순서 변경
        assertThatThrownBy(() -> answerService.submitTextAnswer(SESSION_ID, USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AnswerErrorCode.SESSION_NOT_IN_PROGRESS);
    }

    // ===== 실패 5: 중복 답변 =====

    @Test
    @DisplayName("실패 - 이미 답변했으면 ALREADY_ANSWERED")
    void fail_alreadyAnswered() {
        User user = mockUser(USER_ID);
        Session session = mockSession(user, SessionStatus.IN_PROGRESS);
        Question question = mockQuestion(session);

        given(questionRepository.findById(QUESTION_ID)).willReturn(Optional.of(question));
        given(answerRepository.existsByQuestionId(QUESTION_ID)).willReturn(true);   // 이미 답변

        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, VALID_TRANSCRIPT);

        // 수정: 파라미터 순서 변경
        assertThatThrownBy(() -> answerService.submitTextAnswer(SESSION_ID, USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AnswerErrorCode.ALREADY_ANSWERED);
    }

    // ===== 실패 6: 답변 너무 짧음 =====

    @Test
    @DisplayName("실패 - 답변이 너무 짧으면 ANSWER_TOO_SHORT")
    void fail_tooShort() {
        User user = mockUser(USER_ID);
        Session session = mockSession(user, SessionStatus.IN_PROGRESS);
        Question question = mockQuestion(session);

        given(questionRepository.findById(QUESTION_ID)).willReturn(Optional.of(question));
        given(answerRepository.existsByQuestionId(QUESTION_ID)).willReturn(false);

        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, "짧음");   // 10자 미만

        // 수정: 파라미터 순서 변경
        assertThatThrownBy(() -> answerService.submitTextAnswer(SESSION_ID, USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AnswerErrorCode.ANSWER_TOO_SHORT);
    }
}