package com.myenglishvocab.server.user.service;

import com.myenglishvocab.server.auth.jwt.JwtTokenProvider;
import com.myenglishvocab.server.common.exception.BusinessException;
import com.myenglishvocab.server.common.exception.ErrorCode;
import com.myenglishvocab.server.user.dto.LoginRequest;
import com.myenglishvocab.server.user.dto.LoginResponse;
import com.myenglishvocab.server.user.dto.SignupRequest;
import com.myenglishvocab.server.user.entity.User;
import com.myenglishvocab.server.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @InjectMocks UserService userService;

    @Test
    void 중복_username이면_회원가입_실패() {
        given(userRepository.existsByUsername("hyungyu")).willReturn(true);

        assertThatThrownBy(() -> userService.signup(
                new SignupRequest("hyungyu", "password1", "현규")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_USERNAME);
    }

    @Test
    void 비밀번호가_틀리면_로그인_실패() {
        User user = User.builder()
                .username("hyungyu")
                .password("encoded")
                .displayName("현규")
                .build();

        given(userRepository.findByUsername("hyungyu")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);

        assertThatThrownBy(() -> userService.login(
                new LoginRequest("hyungyu", "wrong")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void 로그인_성공시_accessToken을_반환한다() {
        User user = User.builder()
                .username("hyungyu")
                .password("encoded")
                .displayName("현규")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByUsername("hyungyu")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password1", "encoded")).willReturn(true);
        given(jwtTokenProvider.generateAccessToken(1L, "hyungyu")).willReturn("test-access-token");

        LoginResponse response = userService.login(new LoginRequest("hyungyu", "password1"));

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("hyungyu");
        assertThat(response.displayName()).isEqualTo("현규");
        assertThat(response.accessToken()).isEqualTo("test-access-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(jwtTokenProvider).generateAccessToken(eq(1L), eq("hyungyu"));
    }

    @Test
    void 회원가입_성공시_비밀번호를_해싱해서_저장한다() {
        given(userRepository.existsByUsername("hyungyu")).willReturn(false);
        given(passwordEncoder.encode("password1")).willReturn("hashed");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        userService.signup(new SignupRequest("hyungyu", "password1", "현규"));

        verify(passwordEncoder).encode("password1");
        verify(userRepository).save(any(User.class));
    }
}