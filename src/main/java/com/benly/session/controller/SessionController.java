package com.benly.session.controller;

import com.benly.global.common.ApiResponse;
import com.benly.question.service.QuestionGenerationService;
import com.benly.session.dto.SessionCreateRequest;
import com.benly.session.dto.SessionCreateResponse;
import com.benly.session.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
            @RequestParam Long userId, // TODO: 인증 완성 후 @AuthenticationPrincipal로
            @Valid @RequestBody SessionCreateRequest sessionCreateRequest) {
        // 1. 세션 생성 완료 (이 메서드가 종료되면서 Service 단의 @Transactional이 커밋)
        SessionCreateResponse data = sessionService.createSession(userId, sessionCreateRequest);

        // 2. DB 커밋이 확실하게 보장된 상태에서 비동기 메서드 호출
        questionGenerationService.generate(data.sessionId(), sessionCreateRequest.jobDescription());

        return ApiResponse.success("면접관이 면접을 준비하고 있어.", data);
    }
}
