package com.benly.question.controller;

import com.benly.global.common.ApiResponse;
import com.benly.question.dto.AnswerCreateRequest;
import com.benly.question.dto.AnswerResponse;
import com.benly.question.service.AnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sessions")
public class AnswerController {

    private final AnswerService answerService;

    @PostMapping("/{sessionId}/answers/text")
    public ApiResponse<AnswerResponse> submitTextAnswer(
            @PathVariable Long sessionId,
            @RequestParam Long userId,   // TODO: 인증 완성 후 @AuthenticationPrincipal
            @Valid @RequestBody AnswerCreateRequest request
    ) {
        AnswerResponse data = answerService.submitTextAnswer(sessionId, userId, request);
        return ApiResponse.success("답변이 저장되었습니다.", data);
    }
}
