package com.benly.auth.service;

import com.benly.auth.client.KakaoOAuthClient;
import com.benly.auth.dto.KakaoLoginRequest;
import com.benly.auth.dto.KakaoLoginResponse;
import com.benly.auth.dto.TokenPair;
import com.benly.auth.entity.RefreshToken;
import com.benly.auth.exception.AuthErrorCode;
import com.benly.auth.jwt.JwtProvider;
import com.benly.auth.repository.RefreshTokenRepository;
import com.benly.global.exception.BusinessException;
import com.benly.user.entity.User;
import com.benly.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public KakaoLoginResponse kakaoLogin(KakaoLoginRequest request) {
        String kakaoId = kakaoOAuthClient.getKakaoId(request.authorizationCode());

        boolean isNewUser = !userRepository.existsByKakaoId(kakaoId);
        User user = findOrRegister(kakaoId, request.termsAgreed());

        TokenPair tokens = issueAndSaveTokens(user);

        return KakaoLoginResponse.of(tokens, user, isNewUser);
    }

    private User findOrRegister(String kakaoId, boolean termsAgreed) {
        return userRepository.findByKakaoId(kakaoId)
                .orElseGet(() -> register(kakaoId, termsAgreed));
    }

    private User register(String kakaoId, boolean termsAgreed) {
        if (!Boolean.TRUE.equals(termsAgreed)) {
            throw new BusinessException(AuthErrorCode.TERMS_NOT_AGREED);
        }
        User user = User.of(kakaoId, "카카오사용자");
        return userRepository.save(user);
    }

    private TokenPair issueAndSaveTokens(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        refreshTokenRepository.save(
                RefreshToken.of(user, refreshToken, jwtProvider.getRefreshTokenExpiry()
                ));

        return new TokenPair(accessToken, refreshToken);
    }
}
