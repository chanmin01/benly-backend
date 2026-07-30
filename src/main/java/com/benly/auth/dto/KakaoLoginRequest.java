package com.benly.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record KakaoLoginRequest(
        @NotBlank(message = "인가 코드는 필수입니다.")
        String authorizationCode,

        @NotNull(message = "약관 동의 여부는 필수입니다.")
        Boolean termsAgreed

) {
}
