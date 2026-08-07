package com.benly.session.controller;

import com.benly.global.common.ApiResponse;
import com.benly.question.service.QuestionGenerationService;
import com.benly.session.dto.*;
import com.benly.session.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final QuestionGenerationService questionGenerationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SessionCreateResponse> createSession(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SessionCreateRequest sessionCreateRequest) {
        // 1. 세션 생성 완료 (이 메서드가 종료되면서 Service 단의 @Transactional이 커밋)
        SessionCreateResponse data = sessionService.createSession(userId, sessionCreateRequest);

        // 2. DB 커밋이 확실하게 보장된 상태에서 비동기 메서드 호출
        questionGenerationService.generate(data.sessionId(), sessionCreateRequest.jobDescription());

        return ApiResponse.success("면접관이 면접을 준비하고 있어요.", data);
    }

    @PostMapping("/{sessionId}/start")
    public ApiResponse<SessionStartResponse> startSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Long userId) {
        SessionStartResponse data = sessionService.startSession(userId, sessionId);
        return ApiResponse.success("면접을 시작합니다.", data);
    }


    @GetMapping("/{sessionId}")
    public ApiResponse<SessionDetailResponse> getSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Long userId) {
        SessionDetailResponse data = sessionService.getSession(userId, sessionId);
        return ApiResponse.success("세션을 조회했습니다.", data);
    }

    @GetMapping("/{sessionId}/generation-status")
    public ApiResponse<GenerationStatusResponse> getGenerationStatus(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Long userId) {
        GenerationStatusResponse data = sessionService.getGenerationStatus(userId, sessionId);
        return ApiResponse.success("생성 상태를 조회했습니다.", data);
    }

    @PostMapping("/{sessionId}/analyze")
    public ApiResponse<AnalyzeResponse> analyze(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Long userId) {
        AnalyzeResponse data = sessionService.analyze(userId, sessionId);
        return ApiResponse.success("채점을 요청했습니다.", data);
    }
}
