package com.benly.history.service;

import com.benly.global.exception.BusinessException;
import com.benly.history.dto.SessionHistoryResponse;
import com.benly.history.exception.HistoryErrorCode;
import com.benly.history.repository.SessionHistoryRepository;
import com.benly.session.entity.CompanyType;
import com.benly.session.entity.Session;
import com.benly.session.entity.SessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoryService {

    private static final SessionStatus COMPLETED = SessionStatus.COMPLETED;

    private final SessionHistoryRepository sessionHistoryRepository;

    public SessionHistoryResponse getMySessions(Long userId, String companyType) {
        List<Session> sessions = findCompletedSessions(userId, companyType);

        long totalCount = sessionHistoryRepository.countByUser_IdAndStatus(userId, COMPLETED);
        long weekCount = sessionHistoryRepository
                .countByUser_IdAndStatusAndCreatedAtGreaterThanEqual(userId, COMPLETED, thisWeekStart());

        return SessionHistoryResponse.of(totalCount, weekCount, sessions);

    }

    private List<Session> findCompletedSessions(Long userId, String companyType) {
        if (companyType == null || companyType.isBlank()) {
            return sessionHistoryRepository
                    .findByUser_IdAndStatusOrderByCreatedAtDesc(userId, COMPLETED);
        }
        String validated = validateCompanyType(companyType);
        return sessionHistoryRepository
                .findByUser_IdAndStatusAndCompanyTypeOrderByCreatedAtDesc(userId, COMPLETED, validated);
    }

    private String validateCompanyType(String companyType) {
        try {
            return CompanyType.valueOf(companyType).name();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(HistoryErrorCode.INVALID_COMPANY_TYPE);
        }
    }

    private LocalDateTime thisWeekStart() {
        return LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
    }
}
