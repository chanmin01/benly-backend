package com.benly.question.client;

import com.benly.session.dto.SessionCreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
            RestClient restClient,
            @Value("${claude.api-key}") String apiKey,
            @Value("${claude.api-url}") String apiUrl,
            @Value("${claude.model}") String model
    ) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
    }


    public List<String> generateQuestions(String companyType, String stage, String jobRole, String jd){

        // 1. 프롬프트 만들기
        String prompt = buildPrompt(companyType, stage, jobRole, jd);

        JsonNode response = restClient.post()
                .uri(apiUrl)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .body(Map.of(
                        "model", model,
                        "max_tokens", 2000,
                        "messages", List.of(
                                Map.of("role", "user", "content", prompt)
                        )
                ))
                .retrieve()
                .body(JsonNode.class);

        return parseQuestions(response);
    }

    // TODO: 서류까지 연동은 추후에

    public String buildPrompt(String companyType, String stage, String jobRole, String jd){
        return """
                당신은 면접관입니다. 아래 조건에 맞는 면접 질문 5개를 생성하세요.

                - 기업 유형: %s
                - 면접 단계: %s
                - 직무: %s
                - 채용공고: %s

                규칙:
                - 정확히 5개의 질문을 만드세요
                - 각 질문은 한 줄로, 줄바꿈으로 구분하세요
                - 번호나 불릿 없이 질문 텍스트만 출력하세요
                """.formatted(
                companyType,
                stage,
                jobRole != null ? jobRole : "미지정",
                jd != null ? jd : "없음"
        );
    }

    private List<String> parseQuestions(JsonNode response){
        String text = response.path("content").get(0).path("text").asText(); // Claude 응답 구조: { content: [ { text: "질문들" } ] }

        List<String> questions = new ArrayList<>();

        for(String line : text.split("\n")){
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                questions.add(trimmed);
            }
        }
        return questions;
    }
}
