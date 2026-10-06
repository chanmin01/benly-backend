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

    /** Whisper API 자체 업로드 상한 (25MB) */
    private static final long MAX_AUDIO_BYTES = 25L * 1024 * 1024;

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
        // 긴 오디오는 전사에 시간이 더 걸린다. 3분 분량에서 60초로는 부족한 경우가 있어 상향.
        factory.setReadTimeout(120000);

        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
    }

    public String transcribe(MultipartFile audioFile) {
        // Whisper 상한을 넘는 파일은 호출 전에 걸러낸다.
        // 넘긴 채로 호출하면 OpenAI가 413을 주는데, 그 전에 파일 전송에만 시간을 다 쓴다.
        if (audioFile.getSize() > MAX_AUDIO_BYTES) {
            log.warn("오디오 용량 초과: {} bytes", audioFile.getSize());
            throw new IllegalStateException("오디오 파일이 너무 큽니다.");
        }

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