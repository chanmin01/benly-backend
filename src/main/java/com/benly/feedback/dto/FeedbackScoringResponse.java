package com.benly.feedback.dto;

import com.benly.feedback.entity.FeedbackStatus;


public record FeedbackScoringResponse(
        Long sessionId,
        FeedbackStatus status
) {
    public static FeedbackScoringResponse from(Long sessionId) {
        return new FeedbackScoringResponse(sessionId, FeedbackStatus.SCORING);
    }
}
