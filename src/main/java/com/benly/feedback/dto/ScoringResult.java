package com.benly.feedback.dto;

import java.util.List;
import java.util.Map;

/**
 * LLM 채점 결과를 담는 내부 DTO. (외부 응답 DTO 아님)
 */
public class ScoringResult {

    /**
     * 메인 질문 1개에 대한 채점 결과.
     * - axisScores: 축코드 -> 0~100 원점수 (AxisSet 6축)
     * - content: 카드에 저장될 피드백 (good/weak/next/개선답안/꼬리)
     * - shortTitle: 카드 제목용 요약
     */
    public record MainQuestionScore(
            String shortTitle,
            Map<String, Integer> axisScores,
            FeedbackContent content
    ) {}

    /**
     * 세션 전체 종합 코멘트.
     * - summary: 총평(verdict)
     * - keyCoachingWeakness: 가장 핵심적인 약점 1가지
     * - keyCoachingAction: 그 약점에 대한 구체적 개선 행동
     */
    public record SessionSummary(
            String summary,
            String keyCoachingWeakness,
            String keyCoachingAction
    ) {}

    /**
     * 채점 입력용: 메인 질문 + 답변 + 그에 딸린 꼬리질문들.
     */
    public record MainQuestionInput(
            Integer seq,
            String question,
            String answer,
            List<TailInput> tails
    ) {}

    public record TailInput(
            String strategy,
            String question,
            String answer
    ) {}
}
