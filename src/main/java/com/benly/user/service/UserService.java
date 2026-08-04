package com.benly.user.service;

import com.benly.auth.repository.RefreshTokenRepository;
import com.benly.global.exception.BusinessException;
import com.benly.user.dto.UserMeResponse;
import com.benly.user.entity.User;
import com.benly.user.exception.UserErrorCode;
import com.benly.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void updateNickname(Long userId, String nickname) {
        User user = getActiveUser(userId);
        user.changeNickname(nickname);
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        if (user.isDeleted()) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        user.softDelete();
        refreshTokenRepository.deleteByUserId(userId);
    }

    private User getActiveUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    public UserMeResponse getMyInfo(Long userId) {
        User user = getActiveUser(userId);
        return UserMeResponse.from(user);
    }
}
