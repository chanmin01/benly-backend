package com.benly.auth.controller;

import com.benly.auth.dto.KakaoLoginRequest;
import com.benly.auth.dto.KakaoLoginResponse;
import com.benly.auth.dto.TokenPair;
import com.benly.auth.dto.TokenRefreshRequest;
import com.benly.auth.jwt.JwtProvider;
import com.benly.auth.service.AuthService;
import com.benly.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    @PostMapping("/kakao/login")
    public ApiResponse<KakaoLoginResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
        return ApiResponse.success("로그인에 성공했습니다.", authService.kakaoLogin(request));
    }

    @PostMapping("/token/refresh")
    public ApiResponse<TokenPair> reissue(@Valid @RequestBody TokenRefreshRequest request) {
        return ApiResponse.success("토큰이 재발급되었습니다.", authService.reissue(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal Long userId) {
        authService.logout(userId);
        return ApiResponse.success("로그아웃되었습니다.");
    }
}
