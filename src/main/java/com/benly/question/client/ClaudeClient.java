package com.benly.question.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ClaudeClient {
    private final RestClient restClient;
    private final String apiKey;
    private final String apiUrl;
    private final String model;

    public ClaudeClient(
            @Value("${claude.api-key}") String apiKey,
            @Value("${claude.api-url}") String apiUrl,
            @Value("${claude.model}") String model
    ) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);

        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
    }


    public List<String> generateQuestions(String companyType, String stage, String jobRole, String jd, String docText){

        // 1. 프롬프트 만들기
        String prompt = buildPrompt(companyType, stage, jobRole, jd, docText);

        // 2. Tool Use 스키마 설정 및 요청 바디 구성
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 2000,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "tools", List.of(
                        Map.of(
                                "name", "generate_interview_questions",
                                "description", "면접 질문 5개를 생성하여 배열 형태로 반환합니다.",
                                "input_schema", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "questions", Map.of(
                                                        "type", "array",
                                                        "items", Map.of("type", "string"),
                                                        "description", "면접 질문 텍스트 목록 (정확히 5개)"
                                                )
                                        ),
                                        "required", List.of("questions")
                                )
                        )
                ),
                "tool_choice", Map.of(
                        "type", "tool",
                        "name", "generate_interview_questions"
                )
        );

        JsonNode response = restClient.post()
                .uri(apiUrl)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        return parseQuestions(response);
    }

    // TODO: 서류까지 연동은 추후에

    private String buildPrompt(String companyType, String stage, String jobRole,
                               String jd, String docText) {
        return """
            당신은 면접관입니다. 제공된 도구(Tool)를 사용하여 아래 조건에 맞는 면접 질문 5개를 생성하세요.

            - 기업 유형: %s
            - 면접 단계: %s
            - 직무: %s
            - 채용공고: %s

            [지원자 서류]
            %s

            지원자 서류가 제공된 경우, 서류 내용(경험, 프로젝트, 기술스택)을 바탕으로 
            맞춤형 질문을 우선 생성하세요.
            """.formatted(
                companyType,
                stage,
                jobRole != null ? jobRole : "미지정",
                jd != null ? jd : "없음",
                docText != null ? docText : "제출된 서류 없음"
        );
    }

    private List<String> parseQuestions(JsonNode response){
        List<String> questions = new ArrayList<>();

        // Claude 응답 구조에서 "tool_use" 타입 블록을 탐색하여 안전하게 파싱
        JsonNode contentArray = response.path("content");
        for (JsonNode content : contentArray) {
            if ("tool_use".equals(content.path("type").asText())) {
                JsonNode questionsNode = content.path("input").path("questions");

                if (questionsNode.isArray()) {
                    for (JsonNode qNode : questionsNode) {
                        String q = qNode.asText().trim();
                        if (!q.isEmpty()) {
                            questions.add(q);
                        }
                    }
                }
                break;
            }
        }
        return questions;
    }

    public String generateFollowUp(String context) {
        // 1. 프롬프트
        String prompt = buildFollowUpPrompt(context);

        // 2. Tool Use 스키마 (질문 하나)
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 1000,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "tools", List.of(
                        Map.of(
                                "name", "generate_follow_up_question",
                                "description", "꼬리질문 하나를 생성하여 반환합니다.",
                                "input_schema", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "question", Map.of(
                                                        "type", "string",
                                                        "description", "꼬리질문 텍스트 (하나)"
                                                )
                                        ),
                                        "required", List.of("question")
                                )
                        )
                ),
                "tool_choice", Map.of(
                        "type", "tool",
                        "name", "generate_follow_up_question"
                )
        );

        // 3. API 호출
        JsonNode response = restClient.post()
                .uri(apiUrl)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        return parseFollowUp(response);
    }

    private String buildFollowUpPrompt(String context) {
        return """
            당신은 면접관입니다. 아래는 지금까지의 면접 대화입니다.

            %s

            위 대화를 평가하여 꼬리질문 하나를 생성하세요.
            - 답변이 충분하고 구체적이면: 그 내용을 더 깊이 파고드는 질문을 생성하세요.
            - 답변이 불충분하거나 모호하면: 메인 질문 관점에서 다른 각도의 질문을 생성하세요.

            제공된 도구(Tool)를 사용하여 꼬리질문 하나만 반환하세요.
            """.formatted(context);
    }

    private String parseFollowUp(JsonNode response) {
        JsonNode contentArray = response.path("content");
        for (JsonNode content : contentArray) {
            if ("tool_use".equals(content.path("type").asText())) {
                String question = content.path("input").path("question").asText().trim();
                if (!question.isEmpty()) {
                    return question;
                }
            }
        }
        // 파싱 실패 시 (Claude가 이상하게 응답)
        throw new IllegalStateException("꼬리질문 생성 실패");
    }
}
