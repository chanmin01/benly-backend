package com.benly.auth.dto;

public record TokenPair(
        String accessToken,
        String refreshToken
) {
}
