package com.benly.feedback.service;

import com.benly.feedback.dto.FeedbackContent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeedbackContentParser {

    private final ObjectMapper objectMapper;

    public FeedbackContent parse(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(content, FeedbackContent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("피드백 content 파싱 실패", e);
        }
    }

    public String toJson(FeedbackContent content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("피드백 content 직렬화 실패", e);
        }
    }
}