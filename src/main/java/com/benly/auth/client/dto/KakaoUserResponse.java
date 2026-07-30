package com.benly.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserResponse(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {

    public record KakaoAccount(
            Profile profile
    ) {
        public record Profile(
                String nickname
        ) {
        }
    }
}
