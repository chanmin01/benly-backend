package com.benly.question.client;

import com.benly.session.dto.SessionCreateRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ClaudeClient {
    private final RestClient restClient;
    private final String apiKey;
    private final String apiUrl;

    public List<String> generateQuestions(String companyType, String stage, String jobRole, String jd){

    }

    // TODO: 서류까지 연동은 추후에
}
