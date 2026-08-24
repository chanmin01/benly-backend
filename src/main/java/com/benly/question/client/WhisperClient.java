package com.benly.question.client;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Component
public class WhisperClient {
    private final RestClient restClient;
    private final String apiKey;
    private final String apiUrl;
    private final String model;

    public WhisperClient(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.whisper.api-url}") String apiUrl,
            @Value("${openai.whisper.model}") String model
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(60000);   // 음성이라 좀 길게

        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
    }

    public String transcribe(MultipartFile audioFile) {
        try {
            // multipart/form-data로 파일 전송
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(audioFile.getBytes()) {
                @Override
                public String getFilename() {
                    return audioFile.getOriginalFilename();   // 파일명 필요
                }
            });
            body.add("model", model);
            body.add("language", "ko");   // 한국어

            WhisperResponse response = restClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(WhisperResponse.class);

            if (response == null || response.text() == null) {
                // 단순 NPE 대신 외부 API 연동 에러임을 명확히 알 수 있는 예외를 던집니다.
                throw new IllegalStateException("Whisper API 응답이 비어있습니다.");
            }

            return response.text();

        } catch (IOException e) {
            throw new RuntimeException("음성 파일 읽기 실패", e);
        }
    }

    // Whisper 응답 (text 필드만)
    private record WhisperResponse(String text) {
    }
}
