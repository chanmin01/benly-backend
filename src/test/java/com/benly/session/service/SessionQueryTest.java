package com.benly.session.service;

import com.benly.global.exception.BusinessException;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

    private Session mockSession(User owner, SessionStatus status) {
        Session session = mock(Session.class);
        given(session.getUser()).willReturn(owner);
        given(session.getStatus()).willReturn(status);
        return session;
    }

    // ===== 세션 조회 =====

    @Test
    @DisplayName("세션 조회 성공 - 진행도 포함")
    void getSession_success() {
        User user = mockUser(USER_ID);
        Session session = mockSession(user, SessionStatus.IN_PROGRESS);
        given(session.getId()).willReturn(SESSION_ID);
        given(session.getCompanyType()).willReturn("SERVICE");
        given(session.getStage()).willReturn("TECHNICAL");
        given(session.getCompanyName()).willReturn("카카오");
        given(session.getJobTitle()).willReturn("백엔드");

        given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
        // 진행도: 메인 5개, 미답변 2개 → current 3
        given(questionRepository.countBySessionAndParentIsNull(session)).willReturn(5);
        given(questionRepository.findUnansweredMains(SESSION_ID))
                .willReturn(List.of(mock(com.benly.question.entity.Question.class),
                        mock(com.benly.question.entity.Question.class)));  // 2개

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
        Session session = mockSession(otherUser, SessionStatus.IN_PROGRESS);

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
        Session session = mockSession(user, SessionStatus.READY);

        given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        GenerationStatusResponse response = sessionService.getGenerationStatus(USER_ID, SESSION_ID);

        assertThat(response.status()).isEqualTo("READY");
    }

    // ===== analyze =====

    @Test
    @DisplayName("채점 요청 성공 - COMPLETED → ANALYZING")
    void analyze_success() {
        User user = mockUser(USER_ID);
        Session session = mockSession(user, SessionStatus.COMPLETED);
        given(session.getId()).willReturn(SESSION_ID);

        given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        AnalyzeResponse response = sessionService.analyze(USER_ID, SESSION_ID);

        verify(session).markAnalyzing();   // ANALYZING으로 변경 호출됐나
    }

    @Test
    @DisplayName("채점 요청 실패 - COMPLETED 아니면 SESSION_NOT_COMPLETED")
    void analyze_notCompleted() {
        User user = mockUser(USER_ID);
        Session session = mockSession(user, SessionStatus.IN_PROGRESS);  // 아직 진행 중

        given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionService.analyze(USER_ID, SESSION_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SessionErrorCode.SESSION_NOT_COMPLETED);
    }
}