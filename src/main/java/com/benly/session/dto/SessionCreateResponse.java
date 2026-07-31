package com.benly.session.dto;

public record SessionCreateResponse(
        Long sessionId,
        String status,
        String generationStatusUrl
) {
    public static SessionCreateResponse from(Long sessionId, String status) {
        return new SessionCreateResponse(
                sessionId,
                status,
                "/api/v1/sessions/" + sessionId + "/generation-status"
        );
    }
}
