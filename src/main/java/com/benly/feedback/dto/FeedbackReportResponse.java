package com.benly.feedback.dto;

import java.util.List;

/**
 * 리포트 조회(GET /sessions/{id}/feedback) 응답. 노션 명세 기준.
 */
public record FeedbackReportResponse(
        Long sessionId,
        Meta meta,
        KeyCoaching keyCoaching,
        Integer totalScore,
        String verdict,
        Radar radar,
        List<Card> cards,
        Delivery delivery
) {

    public record Meta(
            String companyType,
            String stage,
            String companyName,
            String jobTitle,
            String createdAt
    ) {}

    public record KeyCoaching(
            String weakness,
            String action
    ) {}

    public record Radar(
            String type,
            List<Axis> axes
    ) {
        public record Axis(
                String axis,
                String label,
                Integer score
        ) {}
    }

    public record Card(
            Long questionId,
            Integer num,
            String shortTitle,
            Integer topicScore,
            String question,
            String answer,
            String good,
            String weak,
            String next,
            String weakAxis,               // 설명 문장
            ImprovedAnswer improvedAnswer, // 메인: before + after
            List<Tail> tails
    ) {}

    // 꼬리 = 메인과 동일 피드백 (점수·shortTitle 없음, strategy 추가, before 없음)
    public record Tail(
            String strategy,
            String question,
            String answer,
            String good,
            String weak,
            String next,
            String weakAxis,
            String improvedAfter           // 꼬리는 after만
    ) {}

    public record ImprovedAnswer(String before, String after) {}

    public record Delivery(
            String speechSpeed,
            String speechSpeedNote,
            Integer fillerWordCount,
            String fillerWordNote
    ) {}
}