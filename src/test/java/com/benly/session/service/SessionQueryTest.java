package com.benly.session.service;

import com.benly.global.exception.BusinessException;
import com.benly.question.dto.CurrentQuestionResponse;
import com.benly.question.entity.Question;
import com.benly.question.repository.QuestionRepository;
import com.benly.session.dto.AnalyzeResponse;
import com.benly.session.dto.GenerationStatusResponse;
import com.benly.session.dto.SessionDetailResponse;
import com.benly.session.entity.Session;
import com.benly.session.entity.SessionStatus;
import com.benly.session.exception.SessionErrorCode;
import com.benly.session.repository.SessionRepository;
import com.benly.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionQueryTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private QuestionRepository questionRepository;

    @InjectMocks
    private SessionService sessionService;

    private static final Long USER_ID = 1L;
    private static final Long SESSION_ID = 10L;

    private User mockUser(Long id) {
        User user = mock(User.class);
        given(user.getId()).willReturn(id);
        return user;
    }

    // P3 리뷰 반영: Mock 대신 실제 Session 객체를 생성하는 헬퍼 메서드
    private Session createRealSession(User owner, SessionStatus status) {
        Session session = Session.create(owner, "SERVICE", "TECHNICAL", "백엔드", "카카오");
        ReflectionTestUtils.setField(session, "id", SESSION_ID);
        ReflectionTestUtils.setField(session, "status", status); // 상태 강제 주입
        return session;
    }

    // ===== 세션 조회 =====

    @Test
    @DisplayName("세션 조회 성공 - 진행도 포함")
    void getSession_success() {
        User user = mockUser(USER_ID);
        Session session = createRealSession(user, SessionStatus.IN_PROGRESS);

        given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        // P2 리뷰 반영: 리스트를 가져오는 대신 COUNT 값 반환을 모킹
        given(questionRepository.countBySessionAndParentIsNull(session)).willReturn(5);
        given(questionRepository.countUnansweredMainsBySessionId(SESSION_ID)).willReturn(2);

        SessionDetailResponse response = sessionService.getSession(USER_ID, SESSION_ID);

        assertThat(response.mainProgress().total()).isEqualTo(5);
        assertThat(response.mainProgress().current()).isEqualTo(3);   // 5 - 2
        assertThat(response.status()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("세션 조회 실패 - 세션 없으면 SESSION_NOT_FOUND")
    void getSession_notFound() {
        given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.getSession(USER_ID, SESSION_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SessionErrorCode.SESSION_NOT_FOUND);
    }

    @Test
    @DisplayName("세션 조회 실패 - 남의 세션이면 SESSION_FORBIDDEN")
    void getSession_forbidden() {
        User otherUser = mockUser(2L);
        Session session = createRealSession(otherUser, SessionStatus.IN_PROGRESS);

        given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionService.getSession(USER_ID, SESSION_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SessionErrorCode.SESSION_FORBIDDEN);
    }

    // ===== 폴링 =====

    @Test
    @DisplayName("생성 상태 폴링 성공 - status 반환")
    void getGenerationStatus_success() {
        User user = mockUser(USER_ID);
        Session session = createRealSession(user, SessionStatus.READY);

        given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        GenerationStatusResponse response = sessionService.getGenerationStatus(USER_ID, SESSION_ID);

        assertThat(response.status()).isEqualTo("READY");
    }

    // ===== analyze =====

    @Test
    @DisplayName("채점 요청 성공 - COMPLETED → ANALYZING")
    void analyze_success() {
        User user = mockUser(USER_ID);
        Session session = createRealSession(user, SessionStatus.COMPLETED);

        given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        // P1 리뷰 반영: 원자적 업데이트 쿼리가 성공적으로(1 반환) 수행됨을 모킹
        given(sessionRepository.updateStatusIfCurrent(SESSION_ID, SessionStatus.COMPLETED, SessionStatus.ANALYZING))
                .willReturn(1);

        sessionService.analyze(USER_ID, SESSION_ID);

        // P3 리뷰 반영: verify()가 아니라, 실제 객체의 상태가 제대로 변경되었는지 단언(assert)
        assertThat(session.getStatus()).isEqualTo(SessionStatus.ANALYZING);
    }

    @Test
    @DisplayName("채점 요청 실패 - COMPLETED 아니면 SESSION_NOT_COMPLETED")
    void analyze_notCompleted() {
        User user = mockUser(USER_ID);
        Session session = createRealSession(user, SessionStatus.IN_PROGRESS);

        given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        // P1 리뷰 반영: 다른 상태이므로 업데이트 실패(0 반환)함을 모킹
        given(sessionRepository.updateStatusIfCurrent(SESSION_ID, SessionStatus.COMPLETED, SessionStatus.ANALYZING))
                .willReturn(0);

        assertThatThrownBy(() -> sessionService.analyze(USER_ID, SESSION_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SessionErrorCode.SESSION_NOT_COMPLETED);
    }

    // ===== 현재 질문 조회 =====

    @Test
    @DisplayName("현재 질문 조회 - 미답변 꼬리 우선")
    void getCurrentQuestion_followUpFirst() {
        User user = mockUser(USER_ID);
        Session session = createRealSession(user, SessionStatus.IN_PROGRESS);
        given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        Question followUp = mock(Question.class);
        given(followUp.getId()).willReturn(200L);
        given(followUp.getSeq()).willReturn(1);
        given(followUp.getContent()).willReturn("꼬리질문");
        given(followUp.getType()).willReturn("FOLLOW_UP");

        // 미답변 꼬리 있음 → 그거 반환
        given(questionRepository.findUnansweredFollowUps(SESSION_ID))
                .willReturn(List.of(followUp));

        CurrentQuestionResponse response = sessionService.getCurrentQuestion(USER_ID, SESSION_ID);

        assertThat(response.questionId()).isEqualTo(200L);
        assertThat(response.type()).isEqualTo("FOLLOW_UP");
    }

    @Test
    @DisplayName("현재 질문 조회 - 꼬리 없으면 미답변 메인")
    void getCurrentQuestion_mainWhenNoFollowUp() {
        User user = mockUser(USER_ID);
        Session session = createRealSession(user, SessionStatus.IN_PROGRESS);
        given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        Question main = mock(Question.class);
        given(main.getId()).willReturn(100L);
        given(main.getSeq()).willReturn(2);
        given(main.getContent()).willReturn("메인질문");
        given(main.getType()).willReturn("MAIN");

        // 미답변 꼬리 없음, 미답변 메인 있음
        given(questionRepository.findUnansweredFollowUps(SESSION_ID)).willReturn(List.of());
        given(questionRepository.findUnansweredMains(SESSION_ID)).willReturn(List.of(main));

        CurrentQuestionResponse response = sessionService.getCurrentQuestion(USER_ID, SESSION_ID);

        assertThat(response.questionId()).isEqualTo(100L);
        assertThat(response.type()).isEqualTo("MAIN");
    }

    // ===== 세션 폐기 =====

    @Test
    @DisplayName("세션 폐기 성공 - IN_PROGRESS → CANCELED")
    void cancelSession_success() {
        User user = mockUser(USER_ID);
        Session session = createRealSession(user, SessionStatus.IN_PROGRESS);
        given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        sessionService.cancelSession(USER_ID, SESSION_ID);

        // 실제 객체의 상태 변경 여부(CANCELED)를 직접 검증
        assertThat(session.getStatus()).isEqualTo(SessionStatus.CANCELED);
    }

    @Test
    @DisplayName("세션 폐기 실패 - COMPLETED는 폐기 불가")
    void cancelSession_completed_fails() {
        User user = mockUser(USER_ID);
        Session session = createRealSession(user, SessionStatus.COMPLETED);  // 완료된 건 폐기 불가
        given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionService.cancelSession(USER_ID, SESSION_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SessionErrorCode.SESSION_CANNOT_CANCEL);
    }
}