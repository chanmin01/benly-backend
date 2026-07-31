package com.benly.user.repository;

import com.benly.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByKakaoId(String kakaoId);

    boolean existsByKakaoId(String kakaoId);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);
}
