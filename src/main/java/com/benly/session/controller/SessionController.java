package com.benly.session.controller;

import com.benly.global.common.ApiResponse;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SessionCreateResponse> createSession(
            @RequestParam Long userId, // TODO: 인증 완성 후 @AuthenticationPrincipal로
            @Valid @RequestBody SessionCreateRequest sessionCreateRequest) {
        SessionCreateResponse data = sessionService.createSession(userId, sessionCreateRequest);
        return ApiResponse.success("면접관이 면접을 준비하고 있어.", data);
    }
}
