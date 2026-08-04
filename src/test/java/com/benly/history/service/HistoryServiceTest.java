package com.benly.history.service;

import com.benly.global.exception.BusinessException;
import com.benly.history.dto.SessionHistoryResponse;
import com.benly.history.exception.HistoryErrorCode;
import com.benly.history.repository.SessionHistoryRepository;
import com.benly.session.entity.SessionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class HistoryServiceTest {

    @Mock
    private SessionHistoryRepository sessionHistoryRepository;

    @InjectMocks
    private HistoryService historyService;

    @Test
    @DisplayName("필터 없이 조회하면 전체 완료 세션과 통계를 반환한다")
    void getMySessionWithoutFilter() {
        // given
        given(sessionHistoryRepository
                .findByUser_IdAndStatusOrderByCreatedAtDesc(1L, SessionStatus.COMPLETED))
                .willReturn(List.of());
        given(sessionHistoryRepository
                .countByUser_IdAndStatus(1L, SessionStatus.COMPLETED))
                .willReturn(12L);
        given(sessionHistoryRepository
                .countByUser_IdAndStatusAndCreatedAtGreaterThanEqual(
                        eq(1L), eq(SessionStatus.COMPLETED), any(LocalDateTime.class)))
                .willReturn(3L);

        // when
        SessionHistoryResponse response = historyService.getMySessions(1L, null);

        // then
        assertThat(response.totalCount()).isEqualTo(12L);
        assertThat(response.weekCount()).isEqualTo(3L);
        verify(sessionHistoryRepository, never())
                .findByUser_IdAndStatusAndCompanyTypeOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    @DisplayName("유효한 기업유형 필터로 조회하면 필터 조회 메서드를 호출한다")
    void getMySessionsWithValidFilter() {
        // given
        given(sessionHistoryRepository
                .findByUser_IdAndStatusAndCompanyTypeOrderByCreatedAtDesc(
                        1L, SessionStatus.COMPLETED, "SERVICE"))
                .willReturn(List.of());
        given(sessionHistoryRepository
                .countByUser_IdAndStatus(1L, SessionStatus.COMPLETED))
                .willReturn(5L);
        given(sessionHistoryRepository
                .countByUser_IdAndStatusAndCreatedAtGreaterThanEqual(
                        eq(1L), eq(SessionStatus.COMPLETED), any(LocalDateTime.class)))
                .willReturn(1L);

        // when
        SessionHistoryResponse response = historyService.getMySessions(1L, "SERVICE");

        // then
        assertThat(response.totalCount()).isEqualTo(5L);
        verify(sessionHistoryRepository)
                .findByUser_IdAndStatusAndCompanyTypeOrderByCreatedAtDesc(
                        1L, SessionStatus.COMPLETED, "SERVICE");
    }

    @Test
    @DisplayName("잘못된 기업유형 필터는 400 예외를 던지고 조회하지 않는다")
    void getMySessionsWithInvalidFilter() {
        // when & then
        assertThatThrownBy(() -> historyService.getMySessions(1L, "INVALID_TYPE"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", HistoryErrorCode.INVALID_COMPANY_TYPE);

        verify(sessionHistoryRepository, never())
                .findByUser_IdAndStatusAndCompanyTypeOrderByCreatedAtDesc(any(), any(), any());
        verify(sessionHistoryRepository, never())
                .findByUser_IdAndStatusOrderByCreatedAtDesc(any(), any());
    }

}
