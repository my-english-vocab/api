package com.myenglishvocab.server.user.service;

import com.myenglishvocab.server.auth.jwt.JwtTokenProvider;
import com.myenglishvocab.server.user.dto.LoginRequest;
import com.myenglishvocab.server.user.dto.LoginResponse;
import com.myenglishvocab.server.user.dto.SignupRequest;
import com.myenglishvocab.server.user.dto.SignupResponse;
import com.myenglishvocab.server.user.entity.User;
import com.myenglishvocab.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용중인 ID입니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User newUser = User.builder()
                .username(request.username())
                .password(encodedPassword)
                .displayName(request.displayName())
                .build();

        User savedUser = userRepository.save(newUser);

        return SignupResponse.from(savedUser);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));

        boolean matches = passwordEncoder.matches(request.password(), user.getPassword());
        if (!matches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

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
