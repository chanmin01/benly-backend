package com.benly.session.dto;

import com.benly.session.entity.Session;

public record GenerationStatusResponse(
        String status
) {
    public static GenerationStatusResponse from(Session session) {
        return new GenerationStatusResponse(session.getStatus().name());
    }
}