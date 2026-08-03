package com.benly.user.service;

import com.benly.auth.entity.RefreshToken;
import com.benly.auth.jwt.JwtProvider;
import com.benly.auth.repository.RefreshTokenRepository;
import com.benly.auth.service.AuthService;
import com.benly.user.entity.User;
import com.benly.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WithdrawReissueConcurrencyTest {

    @Autowired private UserService userService;
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private JwtProvider jwtProvider;

    @Test
    @DisplayName("탈퇴와 토큰 재발급이 동시에 실행돼도 탈퇴 후 토큰이 남지 않는다")
    void withdrawAndReissueConcurrently() throws InterruptedException {
        // given
        User user = userRepository.save(User.of("12345", "홍길동"));
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenRepository.save(
                RefreshToken.of(user, refreshToken, jwtProvider.getRefreshTokenExpiry()));

        // when
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        pool.submit(() -> {
            awaitSilently(start);
            try {
                userService.withdraw(user.getId());
            } catch (Exception ignored) {
            }
        });
        pool.submit(() -> {
            awaitSilently(start);
            try {
                authService.reissue(refreshToken);
            } catch (Exception ignored) {
            }
        });

        start.countDown();
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        // then
        User result = userRepository.findById(user.getId()).orElseThrow();
        assertThat(result.isDeleted()).isTrue();
        assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    private void awaitSilently(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}