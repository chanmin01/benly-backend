package com.benly.question.controller;

import com.benly.global.common.ApiResponse;
import com.benly.question.dto.AnswerResponse;
import com.benly.question.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final AnswerService answerService;

    @PostMapping("/{questionId}/skip")
    public ApiResponse<AnswerResponse> skipQuestion(
            @PathVariable Long questionId,
            @RequestParam Long sessionId,
            @AuthenticationPrincipal Long userId) {
        AnswerResponse data = answerService.skipQuestion(sessionId, userId, questionId);
        return ApiResponse.success("질문을 건너뛰었습니다.", data);
    }
}