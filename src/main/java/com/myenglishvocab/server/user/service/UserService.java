package com.myenglishvocab.server.user.service;

import com.myenglishvocab.server.auth.jwt.JwtTokenProvider;
import com.myenglishvocab.server.auth.token.RefreshTokenStore;
import com.myenglishvocab.server.common.exception.BusinessException;
import com.myenglishvocab.server.common.exception.ErrorCode;
import com.myenglishvocab.server.config.JwtProperties;
import com.myenglishvocab.server.user.dto.*;
import com.myenglishvocab.server.user.entity.User;
import com.myenglishvocab.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final JwtProperties jwtProperties;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            log.warn("회원가입 실패 username={} reason=duplicate", request.username());
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User newUser = User.builder()
                .username(request.username())
                .password(encodedPassword)
                .displayName(request.displayName())
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("회원가입 성공 userId={} username={}", savedUser.getId(), savedUser.getUsername());

        return SignupResponse.from(savedUser);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.warn("로그인 실패 username={} reason=user_not_found", request.username());
                    return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
                });

        boolean matches = passwordEncoder.matches(request.password(), user.getPassword());
        if (!matches) {
            log.warn("로그인 실패 username={} reason=invalid_password", request.username());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        log.info("로그인 성공 userId={} username={}", user.getId(), user.getUsername());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = issueRefreshToken(user.getId());

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                accessToken,
                refreshToken,
                "Bearer"
        );
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        Long userId = refreshTokenStore.findUserId(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        refreshTokenStore.delete(refreshToken);
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
        String newRefreshToken = issueRefreshToken(user.getId());

        log.info("토큰 재발급 성공 userId={}", userId);
        return TokenResponse.of(newAccessToken, newRefreshToken);
    }

    public void logout(String refreshToken) {
        Long userId = refreshTokenStore.findUserId(refreshToken)
                .orElse(null);

        refreshTokenStore.delete(refreshToken);

        if (userId != null) {
            log.info("로그아웃 성공 userId={}", userId);
        }
    }

    private String issueRefreshToken(Long userId) {
        String refreshToken = UUID.randomUUID().toString();
        Duration ttl = Duration.ofMillis(jwtProperties.getRefreshTokenExpiration());
        refreshTokenStore.save(refreshToken, userId, ttl);
        return refreshToken;
    }
}
