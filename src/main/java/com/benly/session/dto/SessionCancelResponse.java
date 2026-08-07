package com.benly.session.dto;

import com.benly.session.entity.Session;

public record SessionCancelResponse(
        Long sessionId,
        String status
) {
    public static SessionCancelResponse from(Session session) {
        return new SessionCancelResponse(session.getId(), session.getStatus().name());
    }
}