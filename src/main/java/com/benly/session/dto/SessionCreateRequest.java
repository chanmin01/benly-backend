package com.benly.session.dto;

import jakarta.validation.constraints.NotBlank;

public record SessionCreateRequest(
        @NotBlank(message = "기업 유형은 필수입니다.")
        String companyType,

        @NotBlank(message = "면접 단계는 필수입니다.")
        String interviewStage,

        String companyName,             // 선택
        String jobRole,                 // 선택
        String jobDescription,          // 선택 (저장 안함, 질문 생성용)
        Long docId                      // 선택
) {
}
