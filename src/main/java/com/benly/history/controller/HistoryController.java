package com.benly.history.controller;

import com.benly.global.common.ApiResponse;
import com.benly.history.dto.SessionHistoryResponse;
import com.benly.history.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/sessions")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping
    public ApiResponse<SessionHistoryResponse> getMySessions(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String companyType
    ) {
        return ApiResponse.success(
                "면접 기록을 조회했어요",
                historyService.getMySessions(userId, companyType));
    }
}
