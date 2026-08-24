package com.benly.feedback.dto;

import java.util.List;

public record FeedbackContent(
        String good,
        String weak,
        String next,
        String weakAxis,
        ImprovedAnswer improvedAnswer,
        List<TailContent> tails
) {
    public record ImprovedAnswer(
            String before,
            String after
    ) {
    }

    public record TailContent(
            String strategy,
            String good,
            String weak,
            String next,
            String weakAxis,
            String improvedAfter
    ) {
    }
}
