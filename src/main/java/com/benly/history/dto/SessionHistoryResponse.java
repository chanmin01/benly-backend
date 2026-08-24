package com.benly.history.dto;

import com.benly.session.entity.Session;

import java.time.LocalDateTime;
import java.util.List;

public record SessionHistoryResponse(
        long totalCount,
        long weekCount,
        List<Item> sessions
) {
    public static SessionHistoryResponse of(long totalCount, long weekCount, List<Session> sessions) {
        List<Item> items = sessions.stream().map(Item::from).toList();
        return new SessionHistoryResponse(totalCount, weekCount, items);
    }

    public record Item(
            Long sessionId,
            String companyType,
            String interviewStage,
            String companyName,
            String jobRole,
            String status,
            LocalDateTime createdAt
    ) {
        public static Item from(Session s) {
            return new Item(
                    s.getId(),
                    s.getCompanyType(),
                    s.getStage(),
                    s.getCompanyName(),
                    s.getJobTitle(),
                    s.getStatus().name(),
                    s.getCreatedAt()
            );
        }
    }
}
