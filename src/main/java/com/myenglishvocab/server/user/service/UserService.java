package com.myenglishvocab.server.user.service;

import com.myenglishvocab.server.analytics.entity.AccountLifecycleEvent;
import com.myenglishvocab.server.analytics.entity.AccountLifecycleType;
import com.myenglishvocab.server.analytics.entity.ActivityType;
import com.myenglishvocab.server.analytics.repository.AccountLifecycleEventRepository;
import com.myenglishvocab.server.analytics.service.ActivityService;
import com.myenglishvocab.server.auth.jwt.JwtTokenProvider;
import com.myenglishvocab.server.auth.token.RefreshTokenStore;
import com.myenglishvocab.server.common.exception.BusinessException;
import com.myenglishvocab.server.common.exception.ErrorCode;
import com.myenglishvocab.server.config.JwtProperties;
import com.myenglishvocab.server.user.dto.*;
import com.myenglishvocab.server.user.entity.User;
import com.myenglishvocab.server.user.entity.UserStatus;
import com.myenglishvocab.server.user.repository.UserRepository;
import com.myenglishvocab.server.quiz.repository.QuizSetAttemptRepository;
import com.myenglishvocab.server.word.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
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
    private final ActivityService activityService;
    private final AccountLifecycleEventRepository lifecycleEventRepository;
    private final WordRepository wordRepository;
    private final QuizSetAttemptRepository quizSetAttemptRepository;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        User existingUser = userRepository.findByUsername(request.username()).orElse(null);
        if (existingUser != null) {
            log.warn("회원가입 실패 username={} reason=duplicate", request.username());
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        Instant now = Instant.now();
        User newUser = User.builder()
                .username(request.username())
                .password(encodedPassword)
                .displayName(request.displayName())
                .build();
        User savedUser = userRepository.save(newUser);

        lifecycleEventRepository.save(new AccountLifecycleEvent(
                savedUser,
                request.username(),
                AccountLifecycleType.SIGNUP,
                now
        ));
        log.info("회원가입 성공 userId={} username={}", savedUser.getId(), savedUser.getUsername());

        return SignupResponse.from(savedUser);
    }

    @Transactional
    public LoginSession login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.warn("로그인 실패 username={} reason=user_not_found", request.username());
                    return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
                });

        if (!user.isActive()) {
            log.warn("로그인 실패 username={} reason=withdrawn", request.username());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        boolean matches = passwordEncoder.matches(request.password(), user.getPassword());
        if (!matches) {
            log.warn("로그인 실패 username={} reason=invalid_password", request.username());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        Instant now = Instant.now();
        user.recordLogin(now);
        activityService.record(user, ActivityType.LOGIN, null, now);
        log.info("로그인 성공 userId={} username={}", user.getId(), user.getUsername());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = issueRefreshToken(user.getId());

        LoginResponse body = new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                accessToken,
                "Bearer"
        );
        return new LoginSession(body, refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshSession refresh(String refreshToken) {
        Long userId = refreshTokenStore.findUserId(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (!user.isActive()) {
            refreshTokenStore.delete(refreshToken);
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        refreshTokenStore.delete(refreshToken);
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
        String newRefreshToken = issueRefreshToken(user.getId());

        log.info("토큰 재발급 성공 userId={}", userId);
        return new RefreshSession(TokenResponse.of(newAccessToken), newRefreshToken);
    }

    public void logout(String refreshToken) {
        Long userId = refreshTokenStore.findUserId(refreshToken)
                .orElse(null);

        refreshTokenStore.delete(refreshToken);

        if (userId != null) {
            log.info("로그아웃 성공 userId={}", userId);
        }
    }

    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        User user = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        wordRepository.deleteByUserId(userId);
        quizSetAttemptRepository.deleteByUserId(userId);
        refreshTokenStore.deleteAllByUserId(userId);

        Instant now = Instant.now();
        String originalUsername = user.getUsername();
        String anonymizedUsername = "withdrawn-" + user.getId() + "-" + UUID.randomUUID();
        String disabledPassword = passwordEncoder.encode(UUID.randomUUID().toString());
        user.withdraw(anonymizedUsername, disabledPassword, now);
        lifecycleEventRepository.save(new AccountLifecycleEvent(
                user,
                originalUsername,
                AccountLifecycleType.WITHDRAWAL,
                now
        ));

        log.info("회원 탈퇴 완료 userId={}", userId);
    }

    private String issueRefreshToken(Long userId) {
        String refreshToken = UUID.randomUUID().toString();
        Duration ttl = Duration.ofMillis(jwtProperties.getRefreshTokenExpiration());
        refreshTokenStore.save(refreshToken, userId, ttl);
        return refreshToken;
    }
}
