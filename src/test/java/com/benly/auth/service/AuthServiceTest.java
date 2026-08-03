package com.benly.auth.service;

import com.benly.auth.client.KakaoOAuthClient;
import com.benly.auth.client.dto.KakaoUserInfo;
import com.benly.auth.dto.KakaoLoginRequest;
import com.benly.auth.dto.KakaoLoginResponse;
import com.benly.auth.entity.RefreshToken;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


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
        given(userRepository.findByKakaoId("12345")).willReturn(Optional.empty());
        given(userRegistrationService.register(any(KakaoUserInfo.class), anyBoolean()))
                .willThrow(new BusinessException(AuthErrorCode.TERMS_NOT_AGREED));

        KakaoLoginRequest request = new KakaoLoginRequest("code", false);

        // when & then
        assertThatThrownBy(() -> authService.kakaoLogin(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.TERMS_NOT_AGREED);
    }

    @Test
    @DisplayName("동시 가입 충돌 시 기존 유저로 복구하고 isNewUser는 false다")
    void concurrentRegisterFallsBackToExisting() {
        // given
        given(kakaoOAuthClient.getKakaoUser("code"))
                .willReturn(new KakaoUserInfo("12345", "홍길동"));
        // 첫 조회: 없음(신규처럼 진입) → 저장 실패 후 재조회: 있음(기존 복구)
        given(userRepository.findByKakaoId("12345"))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(User.of("12345", "홍길동")));
        given(userRegistrationService.register(any(KakaoUserInfo.class), anyBoolean()))
                .willThrow(new DataIntegrityViolationException("duplicate kakao_id"));
        given(jwtProvider.createAccessToken(any())).willReturn("access-token");
        given(jwtProvider.createRefreshToken(any())).willReturn("refresh-token");
        given(jwtProvider.getRefreshTokenExpiry()).willReturn(LocalDateTime.now().plusWeeks(2));

        // when
        KakaoLoginResponse response = authService.kakaoLogin(new KakaoLoginRequest("code", true));

        // then
        assertThat(response.isNewUser()).isFalse();
    }

    @Test
    @DisplayName("탈퇴한 유저가 다시 로그인하면 계정이 복구되고 isNewUser는 false다")
    void withdrawnUserReloginRestoresAccount() {
        // given
        User withdrawnUser = User.of("12345", "홍길동");
        withdrawnUser.softDelete();

        given(kakaoOAuthClient.getKakaoUser("code"))
                .willReturn(new KakaoUserInfo("12345", "홍길동"));
        given(userRepository.findByKakaoId("12345"))
                .willReturn(Optional.of(withdrawnUser));
        given(jwtProvider.createAccessToken(any())).willReturn("access-token");
        given(jwtProvider.createRefreshToken(any())).willReturn("refresh-token");
        given(jwtProvider.getRefreshTokenExpiry())
                .willReturn(LocalDateTime.now().plusWeeks(2));

        // when
        KakaoLoginResponse response = authService.kakaoLogin(new KakaoLoginRequest("code", true));

        // then
        assertThat(withdrawnUser.isDeleted()).isFalse();
        assertThat(response.isNewUser()).isFalse();
    }

    @Test
    @DisplayName("탈퇴한 유저의 토큰 재발급 요청은 거부되고 새 토큰이 저장되지 않는다")
    void reissueRejectsWithdrawnUser() {
        // given
        User withdrawnUser = User.of("12345", "홍길동");
        withdrawnUser.softDelete();
        RefreshToken saved = RefreshToken.of(withdrawnUser, "refresh-token",
                LocalDateTime.now().plusWeeks(2));

        given(jwtProvider.isValid("refresh-token")).willReturn(true);
        given(refreshTokenRepository.findByRefreshToken("refresh-token"))
                .willReturn(Optional.of(saved));
        given(userRepository.findByIdForUpdate(any()))
                .willReturn(Optional.of(withdrawnUser));

        // when & then
        assertThatThrownBy(() -> authService.reissue("refresh-token"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_TOKEN);

        verify(refreshTokenRepository).delete(saved);
        verify(refreshTokenRepository, never()).save(any());
    }
}
