package com.benly.session.service;

import com.benly.question.entity.Question;
import com.benly.question.repository.QuestionRepository;
import com.benly.session.dto.SessionStartResponse;
import com.benly.session.entity.Session;
import com.benly.session.exception.SessionErrorCode;
import com.benly.global.exception.BusinessException;
import com.benly.session.repository.SessionRepository;
import com.benly.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SessionStartServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private QuestionRepository questionRepository;

    @InjectMocks
    private SessionService sessionService;

    @Test
    @DisplayName("면접 시작 성공 - IN_PROGRESS로 바뀌고 첫 질문 반환")
    void startSession_success() {
        // given
        Long userId = 1L;
        Long sessionId = 10L;

        User user = mock(User.class);
        given(user.getId()).willReturn(userId);

        Session session = mock(Session.class);
        given(session.getUser()).willReturn(user);
        given(session.getStatus()).willReturn("READY");   // READY 상태
        given(session.getId()).willReturn(sessionId);

        Question firstQuestion = mock(Question.class);
        given(firstQuestion.getId()).willReturn(100L);
        given(firstQuestion.getSeq()).willReturn(1);
        given(firstQuestion.getContent()).willReturn("첫 질문입니다");

        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(questionRepository.findFirstBySessionAndParentIsNullOrderBySeqAsc(session))
                .willReturn(Optional.of(firstQuestion));

        // when
        SessionStartResponse response = sessionService.startSession(userId, sessionId);

        // then
        verify(session).markInProgress();                    // 상태 변경 호출됐나
        assertThat(response.firstQuestion().questionId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("면접 시작 실패 - READY 상태 아니면 409")
    void startSession_notReady() {
        // given
        Long userId = 1L;
        Long sessionId = 10L;

        User user = mock(User.class);
        given(user.getId()).willReturn(userId);

        Session session = mock(Session.class);
        given(session.getUser()).willReturn(user);
        given(session.getStatus()).willReturn("IN_PROGRESS");  // 이미 진행 중

        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));

        // when & then
        assertThatThrownBy(() -> sessionService.startSession(userId, sessionId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("면접 시작 실패 - 남의 세션이면 403")
    void startSession_forbidden() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long sessionId = 10L;

        User otherUser = mock(User.class);
        given(otherUser.getId()).willReturn(otherUserId);   // 다른 사람

        Session session = mock(Session.class);
        given(session.getUser()).willReturn(otherUser);

        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));

        // when & then
        assertThatThrownBy(() -> sessionService.startSession(userId, sessionId))
                .isInstanceOf(BusinessException.class);
    }
}
