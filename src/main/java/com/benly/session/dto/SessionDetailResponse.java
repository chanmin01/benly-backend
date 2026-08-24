package com.benly.session.dto;

import com.benly.session.entity.Session;

public record SessionDetailResponse(
        Long sessionId,
        String status,
        String companyType,
        String interviewStage,
        String companyName,
        String jobRole,
        MainProgress mainProgress
) {
    public record MainProgress(
            int current,
            int total
    ) {
    }

    public static SessionDetailResponse from(Session session, int current, int total) {
        return new SessionDetailResponse(
                session.getId(),
                session.getStatus().name(),
                session.getCompanyType(),
                session.getStage(),          // 엔티티는 stage, 응답은 interviewStage
                session.getCompanyName(),
                session.getJobTitle(),       // 엔티티는 jobTitle, 응답은 jobRole
                new MainProgress(current, total)
        );
    }
}