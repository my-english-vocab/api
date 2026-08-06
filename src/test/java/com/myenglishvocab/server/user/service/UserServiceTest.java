package com.myenglishvocab.server.user.service;

import com.myenglishvocab.server.auth.jwt.JwtTokenProvider;
import com.myenglishvocab.server.auth.token.RefreshTokenStore;
import com.myenglishvocab.server.common.exception.BusinessException;
import com.myenglishvocab.server.common.exception.ErrorCode;
import com.myenglishvocab.server.config.JwtProperties;
import com.myenglishvocab.server.user.dto.LoginRequest;
import com.myenglishvocab.server.user.dto.LoginResponse;
import com.myenglishvocab.server.user.dto.LoginSession;
import com.myenglishvocab.server.user.dto.RefreshSession;
import com.myenglishvocab.server.user.dto.SignupRequest;
import com.myenglishvocab.server.user.dto.TokenResponse;
import com.myenglishvocab.server.user.entity.User;
import com.myenglishvocab.server.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock RefreshTokenStore refreshTokenStore;
    @Mock JwtProperties jwtProperties;
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
    void 로그인_성공시_accessToken과_refreshToken을_반환한다() {
        User user = User.builder()
                .username("hyungyu")
                .password("encoded")
                .displayName("현규")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByUsername("hyungyu")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password1", "encoded")).willReturn(true);
        given(jwtTokenProvider.generateAccessToken(1L, "hyungyu")).willReturn("test-access-token");
        given(jwtProperties.getRefreshTokenExpiration()).willReturn(604800000L);

        LoginSession session = userService.login(new LoginRequest("hyungyu", "password1"));
        LoginResponse response = session.body();

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("hyungyu");
        assertThat(response.displayName()).isEqualTo("현규");
        assertThat(response.accessToken()).isEqualTo("test-access-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(session.refreshToken()).isNotBlank();

        verify(jwtTokenProvider).generateAccessToken(eq(1L), eq("hyungyu"));
        ArgumentCaptor<String> refreshTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(refreshTokenStore).save(
                refreshTokenCaptor.capture(),
                eq(1L),
                eq(Duration.ofMillis(604800000L))
        );
        assertThat(session.refreshToken()).isEqualTo(refreshTokenCaptor.getValue());
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

    @Test
    void refresh_성공시_새_토큰을_발급하고_기존_refresh를_삭제한다() {
        User user = User.builder()
                .username("hyungyu")
                .password("encoded")
                .displayName("현규")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        given(refreshTokenStore.findUserId("old-refresh")).willReturn(Optional.of(1L));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(jwtTokenProvider.generateAccessToken(1L, "hyungyu")).willReturn("new-access");
        given(jwtProperties.getRefreshTokenExpiration()).willReturn(604800000L);

        RefreshSession session = userService.refresh("old-refresh");
        TokenResponse response = session.body();

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(session.refreshToken()).isNotBlank();
        assertThat(session.refreshToken()).isNotEqualTo("old-refresh");
        assertThat(response.tokenType()).isEqualTo("Bearer");

        verify(refreshTokenStore).delete("old-refresh");
        verify(refreshTokenStore).save(eq(session.refreshToken()), eq(1L), any(Duration.class));
    }

    @Test
    void 존재하지_않는_refresh면_재발급_실패() {
        given(refreshTokenStore.findUserId("invalid-refresh")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.refresh("invalid-refresh"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

        verify(refreshTokenStore, never()).delete(anyString());
    }

    @Test
    void logout_성공시_refreshToken을_삭제한다() {
        given(refreshTokenStore.findUserId("refresh-1")).willReturn(Optional.of(1L));

        userService.logout("refresh-1");

        verify(refreshTokenStore).delete("refresh-1");
    }

    @Test
    void 없는_refresh로_logout해도_예외없이_삭제_시도한다() {
        given(refreshTokenStore.findUserId("missing")).willReturn(Optional.empty());

        userService.logout("missing");

        verify(refreshTokenStore).delete("missing");
    }
}
