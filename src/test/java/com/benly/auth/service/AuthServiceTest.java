package com.benly.auth.service;

import com.benly.auth.client.KakaoOAuthClient;
import com.benly.auth.client.dto.KakaoUserInfo;
import com.benly.auth.dto.KakaoLoginRequest;
import com.benly.auth.dto.KakaoLoginResponse;
import com.benly.auth.exception.AuthErrorCode;
import com.benly.auth.jwt.JwtProvider;
import com.benly.auth.repository.RefreshTokenRepository;
import com.benly.global.exception.BusinessException;
import com.benly.user.entity.User;
import com.benly.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private KakaoOAuthClient kakaoOAuthClient;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private UserRegistrationService userRegistrationService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("신규 유저가 약관 동의하면 가입되고 토큰이 발급된다")
    void newUserLogin() {
        // given
        given(kakaoOAuthClient.getKakaoUser("code"))
                .willReturn(new KakaoUserInfo("12345", "홍길동"));
        given(userRepository.existsByKakaoId("12345")).willReturn(false);
        given(userRepository.findByKakaoId("12345")).willReturn(Optional.empty());
        given(userRegistrationService.register(any(KakaoUserInfo.class), anyBoolean()))
                .willReturn(User.of("12345", "홍길동"));
        given(jwtProvider.createAccessToken(any())).willReturn("access-token");
        given(jwtProvider.createRefreshToken(any())).willReturn("refresh-token");
        given(jwtProvider.getRefreshTokenExpiry())
                .willReturn(LocalDateTime.now().plusWeeks(2));

        KakaoLoginRequest request = new KakaoLoginRequest("code", true);

        // when
        KakaoLoginResponse response = authService.kakaoLogin(request);

        // then
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.isNewUser()).isTrue();
    }

    @Test
    @DisplayName("신규 유저가 약관 미동의하면 예외가 발생한다")
    void newUserWithoutTermsAgreed() {
        // given
        given(kakaoOAuthClient.getKakaoUser("code"))
                .willReturn(new KakaoUserInfo("12345", "홍길동"));
        given(userRepository.existsByKakaoId("12345")).willReturn(false);
        given(userRepository.findByKakaoId("12345")).willReturn(Optional.empty());
        given(userRegistrationService.register(any(KakaoUserInfo.class), anyBoolean()))
                .willThrow(new BusinessException(AuthErrorCode.TERMS_NOT_AGREED));

        KakaoLoginRequest request = new KakaoLoginRequest("code", false);

        // when & then
        assertThatThrownBy(() -> authService.kakaoLogin(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.TERMS_NOT_AGREED);
    }
}
