package com.benly.feedback.event;

public record ScoringRequestedEvent(
        Long sessionId
) {}