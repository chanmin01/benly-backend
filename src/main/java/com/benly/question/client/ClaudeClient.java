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


    public List<String> generateQuestions(String companyType, String stage, String jobRole, String jd){

        // 1. 프롬프트 만들기
        String prompt = buildPrompt(companyType, stage, jobRole, jd);

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

    private String buildPrompt(String companyType, String stage, String jobRole, String jd){
        return """
                당신은 면접관입니다. 제공된 도구(Tool)를 사용하여 아래 조건에 맞는 면접 질문 5개를 생성하세요.

                - 기업 유형: %s
                - 면접 단계: %s
                - 직무: %s
                - 채용공고: %s
                """.formatted(
                companyType,
                stage,
                jobRole != null ? jobRole : "미지정",
                jd != null ? jd : "없음"
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
}
