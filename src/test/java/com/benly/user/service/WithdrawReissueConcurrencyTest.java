package com.benly.user.service;

import com.benly.auth.entity.RefreshToken;
import com.benly.auth.exception.AuthErrorCode;
import com.benly.auth.jwt.JwtProvider;
import com.benly.auth.repository.RefreshTokenRepository;
import com.benly.auth.service.AuthService;
import com.benly.global.exception.BusinessException;
import com.benly.user.entity.User;
import com.benly.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
class WithdrawReissueConcurrencyTest {

    @Autowired
    private UserService userService;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("탈퇴와 토큰 재발급이 동시에 실행돼도 탈퇴 후 토큰이 남지 않는다")
    void withdrawAndReissueConcurrently() throws InterruptedException, ExecutionException {
        // given
        User user = userRepository.save(User.of("12345", "홍길동"));
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenRepository.save(
                RefreshToken.of(user, refreshToken, jwtProvider.getRefreshTokenExpiry()));

        // when
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<?> withdrawTask = pool.submit(() -> {
            awaitSilently(start);
            userService.withdraw(user.getId());
        });
        Future<?> reissueTask = pool.submit(() -> {
            awaitSilently(start);
            authService.reissue(refreshToken);
        });

        start.countDown();
        pool.shutdown();
        boolean finished = pool.awaitTermination(5, TimeUnit.SECONDS);

        // then 1: 시간 내에 두 작업이 끝나야 한다
        assertThat(finished).isTrue();

        // then 2: 탈퇴는 반드시 정상 완료돼야 한다
        assertThatCode(withdrawTask::get).doesNotThrowAnyException();

        // then 3: 재발급은 성공하거나, 탈퇴 유저라 INVALID_TOKEN으로만 실패해야 한다
        try {
            reissueTask.get();
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(BusinessException.class);
            assertThat(((BusinessException) e.getCause()).getErrorCode())
                    .isEqualTo(AuthErrorCode.INVALID_TOKEN);
        }

        // then 4: 최종 상태 — 유저는 탈퇴, 살아남은 토큰 0개
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