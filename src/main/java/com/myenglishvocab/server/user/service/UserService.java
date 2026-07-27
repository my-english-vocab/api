package com.myenglishvocab.server.user.service;

import com.myenglishvocab.server.auth.jwt.JwtTokenProvider;
import com.myenglishvocab.server.common.exception.BusinessException;
import com.myenglishvocab.server.common.exception.ErrorCode;
import com.myenglishvocab.server.user.dto.LoginRequest;
import com.myenglishvocab.server.user.dto.LoginResponse;
import com.myenglishvocab.server.user.dto.SignupRequest;
import com.myenglishvocab.server.user.dto.SignupResponse;
import com.myenglishvocab.server.user.entity.User;
import com.myenglishvocab.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

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

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                accessToken,
                "Bearer"
        );
    }
}
