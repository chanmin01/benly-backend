package com.benly.user.dto;

import com.benly.user.entity.User;

public record UserMeResponse(
        Long id,
        String nickname
) {
    public static UserMeResponse from(User user) {
        return new UserMeResponse(user.getId(), user.getNickname());
    }
}
