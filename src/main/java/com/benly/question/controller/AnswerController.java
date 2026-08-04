package com.benly.question.controller;

import com.benly.global.common.ApiResponse;
import com.benly.question.dto.AnswerCreateRequest;
import com.benly.question.dto.AnswerResponse;
import com.benly.question.service.AnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sessions")
public class AnswerController {

    private final AnswerService answerService;

    @PostMapping("/{sessionId}/answers/text")
    public ApiResponse<AnswerResponse> submitTextAnswer(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AnswerCreateRequest request
    ) {
        AnswerResponse data = answerService.submitTextAnswer(sessionId, userId, request);
        return ApiResponse.success("답변이 저장되었습니다.", data);
    }

    @PostMapping("/{sessionId}/answers/audio")
    public ApiResponse<AnswerResponse> submitAudioAnswer(
            @PathVariable Long sessionId,
            @RequestParam Long questionId,
            @AuthenticationPrincipal Long userId,
            @RequestParam("audio") MultipartFile audioFile) {
        AnswerResponse data = answerService.submitAudioAnswer(
                sessionId, userId, questionId, audioFile);
        return ApiResponse.success("음성 답변이 저장되었습니다.", data);
    }
}
