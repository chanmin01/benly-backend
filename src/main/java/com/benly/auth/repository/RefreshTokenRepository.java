package com.benly.auth.repository;

import com.benly.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    void deleteByUserId(Long userId);

    Optional<RefreshToken> findByUserId(Long userId);

}
