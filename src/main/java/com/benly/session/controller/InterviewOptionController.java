package com.benly.session.controller;

import com.benly.global.common.ApiResponse;
import com.benly.session.dto.InterviewOptionResponse;
import com.benly.session.service.InterviewOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InterviewOptionController {

    private final InterviewOptionService interviewOptionService;

    @GetMapping("/interview-options")
    public ApiResponse<InterviewOptionResponse> getOptions(){
        InterviewOptionResponse data = interviewOptionService.getOptions();
        return ApiResponse.success("설정 선택지를 조회했습니다.",data);

    }
}
