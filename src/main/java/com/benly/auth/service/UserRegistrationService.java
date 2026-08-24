package com.benly.auth.service;

import com.benly.auth.client.dto.KakaoUserInfo;
import com.benly.auth.exception.AuthErrorCode;
import com.benly.global.exception.BusinessException;
import com.benly.user.entity.User;
import com.benly.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private static final String DEFAULT_NICKNAME = "카카오사용자";

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User register(KakaoUserInfo kakaoUser, boolean termsAgreed) {
        if (!Boolean.TRUE.equals(termsAgreed)) {
            throw new BusinessException(AuthErrorCode.TERMS_NOT_AGREED);
        }
        String nickname = resolveNickname(kakaoUser.nickname());
        return userRepository.save(User.of(kakaoUser.kakaoId(), nickname));
    }

    private String resolveNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return DEFAULT_NICKNAME;
        }
        return nickname.length() > 50 ? nickname.substring(0, 50) : nickname;
    }
}
