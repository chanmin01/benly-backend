package com.benly.auth.dto;

import com.benly.user.entity.User;

public record UserResolution(
        User user,
        boolean isNewUser
) {
}
