package com.benly.feedback.dto;

import com.benly.feedback.entity.FeedbackStatus;

public record FeedbackStatusResponse(
        Long sessionId,
        FeedbackStatus status // SCORRING / COMPLETED / FAILED
) {
    public static FeedbackStatusResponse of(Long sessionId, FeedbackStatus status) {
        return new FeedbackStatusResponse(sessionId, status);
    }
}
