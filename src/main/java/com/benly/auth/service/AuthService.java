package com.benly.auth.service;

import com.benly.auth.client.KakaoOAuthClient;
import com.benly.auth.client.dto.KakaoUserInfo;
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

    private static final String DEFAULT_NICKNAME = "카카오사용자";

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public KakaoLoginResponse kakaoLogin(KakaoLoginRequest request) {
        KakaoUserInfo kakaoUser = kakaoOAuthClient.getKakaoUser(request.authorizationCode());

        boolean isNewUser = !userRepository.existsByKakaoId(kakaoUser.kakaoId());
        User user = findOrRegister(kakaoUser, request.termsAgreed());

        TokenPair tokens = issueAndSaveTokens(user);

        return KakaoLoginResponse.of(tokens, user, isNewUser);
    }

    private User findOrRegister(KakaoUserInfo kakaoUser, boolean termsAgreed) {
        return userRepository.findByKakaoId(kakaoUser.kakaoId())
                .orElseGet(() -> register(kakaoUser, termsAgreed));
    }

    private User register(KakaoUserInfo kakaoUser, boolean termsAgreed) {
        if (!Boolean.TRUE.equals(termsAgreed)) {
            throw new BusinessException(AuthErrorCode.TERMS_NOT_AGREED);
        }
        String nickname = resolveNickname(kakaoUser.nickname());
        User user = User.of(kakaoUser.kakaoId(), nickname);
        return userRepository.save(user);
    }

    private String resolveNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return DEFAULT_NICKNAME;
        }
        return nickname.length() > 50 ? nickname.substring(0, 50) : nickname;
    }

    private TokenPair issueAndSaveTokens(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        refreshTokenRepository.save(
                RefreshToken.of(user, refreshToken, jwtProvider.getRefreshTokenExpiry()
                ));

        return new TokenPair(accessToken, refreshToken);
    }

    @Transactional
    public TokenPair reissue(String refreshToken) {
        if (!jwtProvider.isValid(refreshToken)) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }

        RefreshToken saved = refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_TOKEN));

        User user = saved.getUser();
        refreshTokenRepository.delete(saved);

        return issueAndSaveTokens(user);
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
