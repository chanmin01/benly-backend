package com.benly.user.controller;

import com.benly.global.common.ApiResponse;
import com.benly.user.dto.UpdateNicknameRequest;
import com.benly.user.dto.UserMeResponse;
import com.benly.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping("/me")
    public ApiResponse<Void> updateNickname(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateNicknameRequest request
    ) {
        userService.updateNickname(userId, request.nickname());
        return ApiResponse.success("닉네임이 변경되었습니다.");
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(@AuthenticationPrincipal Long userId) {
        userService.withdraw(userId);
        return ApiResponse.success("회원 탈퇴가 완료되었습니다.");
    }

    @GetMapping("/me")
    public ApiResponse<UserMeResponse> getMyInfo(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success("내 정보를 조회했습니다.", userService.getMyInfo(userId));
    }
}


