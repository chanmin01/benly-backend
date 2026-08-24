package com.benly.feedback.controller;

import com.benly.feedback.dto.FeedbackReportResponse;
import com.benly.feedback.dto.FeedbackScoringResponse;
import com.benly.feedback.dto.FeedbackStatusResponse;
import com.benly.feedback.service.FeedbackQueryService;
import com.benly.feedback.service.FeedbackRequestService;
import com.benly.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class FeedbackController {
    private final FeedbackQueryService feedbackQueryService;
    private final FeedbackRequestService feedbackRequestService;

    @PostMapping("/{sessionId}/feedback")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<FeedbackScoringResponse> createScoring(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId) {
        FeedbackScoringResponse data = feedbackRequestService.requestScoring(userId, sessionId);
        return ApiResponse.success("채점을 시작했어요.", data);
    }

    @GetMapping("/{sessionId}/feedback/status")
    public ApiResponse<FeedbackStatusResponse> getStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId
    ) {
        FeedbackStatusResponse data = feedbackQueryService.getStatus(userId, sessionId);
        return ApiResponse.success(data.status().getMessage(), data);
    }

    @GetMapping("/{sessionId}/feedback")
    public ApiResponse<FeedbackReportResponse> getReport(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId) {
        return ApiResponse.success("조회에 성공했습니다.",
                feedbackQueryService.getReport(userId, sessionId));
    }

}
