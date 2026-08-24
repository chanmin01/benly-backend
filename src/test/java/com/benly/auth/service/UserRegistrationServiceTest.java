package com.benly.auth.service;

import com.benly.auth.client.dto.KakaoUserInfo;
import com.benly.user.entity.User;
import com.benly.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class UserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserRegistrationService userRegistrationService;

    @Test
    @DisplayName("카카오 닉네임이 있으면 그 닉네임으로 가입된다")
    void registerWithNickname() {
        // given
        given(userRepository.save(any(User.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        userRegistrationService.register(new KakaoUserInfo("12345", "홍길동"), true);

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("카카오 닉네임이 없으면 기본 닉네임으로 가입된다")
    void registerWithNullNickname() {
        // given
        given(userRepository.save(any(User.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        userRegistrationService.register(new KakaoUserInfo("12345", null), true);

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo("카카오사용자");
    }
}
