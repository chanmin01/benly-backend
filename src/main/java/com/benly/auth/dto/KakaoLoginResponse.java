package com.benly.auth.dto;

import com.benly.user.entity.User;

import java.time.LocalDateTime;

public record KakaoLoginResponse(
        String accessToken,
        String refreshToken,
        Boolean isNewUser,
        UserInfo user
) {
    public record UserInfo(
            Long id,
            String nickname,
            LocalDateTime createdAt
    ) {
        public static UserInfo from(User user) {
            return new UserInfo(user.getId(), user.getNickname(), user.getCreatedAt());
        }
    }

    public static KakaoLoginResponse of(TokenPair tokens, User user, boolean isNewUser) {
        return new KakaoLoginResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                isNewUser,
                UserInfo.from(user)
        );
    }
}
