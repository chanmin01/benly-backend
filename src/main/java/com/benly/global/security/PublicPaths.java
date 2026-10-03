package com.benly.global.security;

public final class PublicPaths {
    private PublicPaths() {}

    public static final String[] AUTH = {
            "/api/v1/auth/kakao/login",
            "/api/v1/auth/token/refresh"
    };
}