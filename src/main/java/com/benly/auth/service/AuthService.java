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
import org.springframework.dao.DataIntegrityViolationException;
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
    private final UserRegistrationService userRegistrationService;

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
                .orElseGet(() -> registerOrFindExisting(kakaoUser, termsAgreed));
    }

    private User registerOrFindExisting(KakaoUserInfo kakaoUser, boolean termsAgreed) {
        try {
            return userRegistrationService.register(kakaoUser, termsAgreed);
        } catch (DataIntegrityViolationException e) {
            return userRepository.findByKakaoId(kakaoUser.kakaoId())
                    .orElseThrow(() -> new BusinessException(AuthErrorCode.KAKAO_AUTH_FAILED));
        }
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
