package com.benly.feedback.client;

import com.benly.feedback.dto.FeedbackContent;
import com.benly.feedback.dto.ScoringContext;
import com.benly.feedback.dto.ScoringResult.MainQuestionInput;
import com.benly.feedback.dto.ScoringResult.MainQuestionScore;
import com.benly.feedback.dto.ScoringResult.SessionSummary;
import com.benly.feedback.dto.ScoringResult.TailInput;
import com.benly.feedback.entity.Axis;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 면접 답변 채점용 Claude 호출 클라이언트.
 * <p>
 * 채점 기준(구조화 면접/STAR 근거):
 * - 각 축을 0~100으로 평가. 답변이 구체적 상황·본인 기여·행동·정량적 결과를 담을수록 고득점.
 * - 모호하거나 일반론에 그치면 저득점.
 * - 축 정의(code/label)는 AxisSet에서 세션 단계·기업유형별로 주입받는다.
 */
@Slf4j
@Component
public class FeedbackScoringClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String apiUrl;
    private final String model;

    public FeedbackScoringClient(
            @Value("${claude.api-key}") String apiKey,
            @Value("${claude.api-url}") String apiUrl,
            @Value("${claude.model}") String model
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(180000);

        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
    }

    /**
     * 메인 질문 1개(+꼬리들)를 채점한다.
     *
     * @param axes    이 세션에 적용할 6개 평가축 (code/label/weight)
     * @param context 면접 메타(기업유형/단계/직무)
     */
    public MainQuestionScore scoreMainQuestion(List<Axis> axes, ScoringContext context, MainQuestionInput input) {
        String prompt = buildMainPrompt(axes, context, input);

        Map<String, Object> axisScoreSchema = new HashMap<>();
        for (Axis axis : axes) {
            axisScoreSchema.put(axis.code(), Map.of(
                    "type", "integer",
                    "description", axis.label() + " 점수 (0~100)"
            ));
        }

        Map<String, Object> tailItemSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "strategy", Map.of("type", "string", "description", "꼬리질문 전략(원문 strategy 값 그대로)"),
                        "good", Map.of("type", "string", "description", "잘한 점"),
                        "weak", Map.of("type", "string", "description", "아쉬운 점"),
                        "next", Map.of("type", "string", "description", "다음에 개선할 방향"),
                        "weakAxis", Map.of("type", "string", "description", "가장 약한 축에 대한 한 문장 설명"),
                        "improvedAfter", Map.of("type", "string", "description", "개선된 모범 답변")
                ),
                "required", List.of("strategy", "good", "weak", "next", "weakAxis", "improvedAfter")
        );

        Map<String, Object> inputSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "shortTitle", Map.of("type", "string", "description", "이 문항을 한 줄로 요약한 카드 제목"),
                        "axisScores", Map.of(
                                "type", "object",
                                "properties", axisScoreSchema,
                                "required", axes.stream().map(Axis::code).collect(Collectors.toList())
                        ),
                        "good", Map.of("type", "string", "description", "메인 답변에서 잘한 점"),
                        "weak", Map.of("type", "string", "description", "메인 답변에서 아쉬운 점"),
                        "next", Map.of("type", "string", "description", "다음에 개선할 방향"),
                        "weakAxis", Map.of("type", "string", "description", "가장 약한 축에 대한 한 문장 설명"),
                        "improvedBefore", Map.of("type", "string", "description", "원답변 핵심 요약(before)"),
                        "improvedAfter", Map.of("type", "string", "description", "개선된 모범 답변(after)"),
                        "tails", Map.of(
                                "type", "array",
                                "items", tailItemSchema,
                                "description", "꼬리질문별 피드백 (꼬리 없으면 빈 배열)"
                        )
                ),
                "required", List.of("shortTitle", "axisScores", "good", "weak", "next", "weakAxis",
                        "improvedBefore", "improvedAfter", "tails")
        );

        JsonNode toolInput = callTool(prompt, "score_answer",
                "면접 답변을 채점하여 축별 점수와 피드백을 반환합니다.", inputSchema, 4000);

        return parseMainScore(axes, toolInput);
    }

    /**
     * 문항별 점수/피드백을 종합하여 세션 총평·핵심 코칭을 생성한다.
     */
    public SessionSummary summarize(ScoringContext context, int totalScore,
                                    List<MainQuestionScore> perQuestion, int totalSkipped) {
        String prompt = buildSummaryPrompt(context, totalScore, perQuestion, totalSkipped);

        Map<String, Object> inputSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "summary", Map.of("type", "string", "description", "면접 전체 총평 (2~3문장, 500자 이내)"),
                        "keyCoachingWeakness", Map.of("type", "string", "description", "가장 핵심적인 약점 1가지"),
                        "keyCoachingAction", Map.of("type", "string", "description", "그 약점을 개선할 구체적 행동 1가지")
                ),
                "required", List.of("summary", "keyCoachingWeakness", "keyCoachingAction")
        );

        JsonNode toolInput = callTool(prompt, "summarize_session",
                "면접 전체를 종합하여 총평과 핵심 코칭을 반환합니다.", inputSchema, 1500);

        return new SessionSummary(
                text(toolInput, "summary"),
                text(toolInput, "keyCoachingWeakness"),
                text(toolInput, "keyCoachingAction")
        );
    }

    private JsonNode callTool(String prompt, String toolName, String toolDesc,
                              Map<String, Object> inputSchema, int maxTokens) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "tools", List.of(Map.of(
                        "name", toolName,
                        "description", toolDesc,
                        "input_schema", inputSchema
                )),
                "tool_choice", Map.of("type", "tool", "name", toolName)
        );

        JsonNode response = restClient.post()
                .uri(apiUrl)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        for (JsonNode content : response.path("content")) {
            if ("tool_use".equals(content.path("type").asText())) {
                return content.path("input");
            }
        }
        throw new IllegalStateException("채점 응답 파싱 실패: tool_use 블록 없음");
    }

    private MainQuestionScore parseMainScore(List<Axis> axes, JsonNode in) {
        Map<String, Integer> axisScores = new HashMap<>();
        JsonNode scoresNode = in.path("axisScores");
        for (Axis axis : axes) {
            axisScores.put(axis.code(), clamp(parseInt(scoresNode, axis.code())));
        }

        List<FeedbackContent.TailContent> tails = new ArrayList<>();
        for (JsonNode t : in.path("tails")) {
            tails.add(new FeedbackContent.TailContent(
                    text(t, "strategy"),
                    text(t, "good"),
                    text(t, "weak"),
                    text(t, "next"),
                    text(t, "weakAxis"),
                    text(t, "improvedAfter")
            ));
        }

        FeedbackContent content = new FeedbackContent(
                text(in, "good"),
                text(in, "weak"),
                text(in, "next"),
                text(in, "weakAxis"),
                new FeedbackContent.ImprovedAnswer(text(in, "improvedBefore"), text(in, "improvedAfter")),
                tails
        );

        return new MainQuestionScore(text(in, "shortTitle"), axisScores, content);
    }

    private String buildMainPrompt(List<Axis> axes, ScoringContext ctx, MainQuestionInput input) {
        String axisGuide = axes.stream()
                .map(a -> "- %s(%s): 가중치 %d%%".formatted(a.label(), a.code(), a.weight()))
                .collect(Collectors.joining("\n"));

        StringBuilder tailText = new StringBuilder();
        if (input.tails() != null) {
            for (TailInput t : input.tails()) {
                tailText.append("""
                        [꼬리질문 - strategy=%s]
                        Q: %s
                        A: %s
                        """.formatted(nvl(t.strategy()), nvl(t.question()), nvl(t.answer())));
            }
        }

        return """
                당신은 %s %s 면접의 숙련된 면접관입니다. (직무: %s)
                아래 지원자의 답변을 구조화 면접 기준으로 엄격하고 공정하게 채점하세요.
                
                [채점 원칙]
                - 각 평가축을 0~100으로 채점합니다.
                - 구체적 상황·본인의 기여·실제 행동·정량적 결과가 드러날수록 높게 채점합니다.
                - 일반론, 모호한 표현, 근거 없는 주장은 낮게 채점합니다.
                - 일부 답변은 비어 있을 수 있습니다(지원자가 건너뜀). 건너뜀 자체는 감점하지 말고,
                  실제로 제출된 답변의 품질만 평가하세요. 건너뛰기에 대한 감점은 시스템이 별도로 처리합니다.
                - 피드백은 지원자가 바로 실천할 수 있도록 구체적으로 작성합니다.
                - 각 피드백(good/weak/next)은 2~3문장 이내로 간결하게 작성합니다.
                - 모범답안(improvedAfter)은 4~5문장 이내로 핵심만 작성합니다.
                - 꼬리질문(tails)의 각 항목도 반드시 채웁니다.
                
                [평가축]
                %s
                
                [메인 질문]
                Q: %s
                A: %s
                %s
                제공된 도구(score_answer)로만 결과를 반환하세요.
                """.formatted(
                nvl(ctx.companyType()), nvl(ctx.stage()), nvl(ctx.jobTitle()),
                axisGuide,
                nvl(input.question()), nvl(input.answer()),
                tailText.toString()
        );
    }

    private String buildSummaryPrompt(ScoringContext ctx, int totalScore,
                                      List<MainQuestionScore> perQuestion, int totalSkipped) {
        String perQ = perQuestion.stream()
                .map(q -> "- %s: 약점=%s".formatted(nvl(q.shortTitle()), nvl(q.content().weak())))
                .collect(Collectors.joining("\n"));

        String skipNote = totalSkipped > 0
                ? "지원자는 총 %d개 질문을 건너뛰었습니다. 총평에 이 점을 반영하세요.".formatted(totalSkipped)
                : "건너뛴 질문은 없습니다.";

        return """
                당신은 %s %s 면접관입니다. 아래는 지원자의 문항별 채점 요약이며, 총점은 %d점(100점 만점)입니다.
                %s
                
                %s
                
                이를 종합하여 면접 전체 총평과, 가장 우선적으로 개선해야 할 핵심 약점 1가지 및 그 개선 행동을 제시하세요.
                제공된 도구(summarize_session)로만 결과를 반환하세요.
                """.formatted(nvl(ctx.companyType()), nvl(ctx.stage()), totalScore, skipNote, perQ);
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private int parseInt(JsonNode node, String field) {
        String v = text(node, field);
        if (v == null || v.isBlank()) {
            return 0;
        }
        try {
            // 소수점/공백 등이 섞여 와도 방어적으로 파싱
            return (int) Math.round(Double.parseDouble(v.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
