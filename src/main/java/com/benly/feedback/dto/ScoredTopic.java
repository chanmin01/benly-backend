package com.benly.feedback.dto;

import com.benly.feedback.dto.ScoringResult.MainQuestionScore;

public record ScoredTopic(
        Long mainQuestionId,
        MainQuestionScore score,
        int topicScore,
        int skippedCount
) {}