package com.benly.session.dto;

import com.benly.session.entity.Session;

public record AnalyzeResponse(
        Long sessionId,
        String status
) {
    public static AnalyzeResponse from(Session session) {
        return new AnalyzeResponse(session.getId(), session.getStatus().name());
    }
}