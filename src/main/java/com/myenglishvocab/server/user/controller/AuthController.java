package com.myenglishvocab.server.user.controller;

import com.myenglishvocab.server.auth.cookie.AuthCookieFactory;
import com.myenglishvocab.server.auth.jwt.JwtPrincipal;
import com.myenglishvocab.server.common.exception.BusinessException;
import com.myenglishvocab.server.common.exception.ErrorCode;
import com.myenglishvocab.server.common.exception.ErrorResponse;
import com.myenglishvocab.server.config.JwtProperties;
import com.myenglishvocab.server.user.dto.*;
import com.myenglishvocab.server.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Tag(name = "Auth", description = "회원가입/로그인/토큰 재발급/로그아웃/내 정보")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtProperties jwtProperties;

    @Value("${auth.cookie.secure:false}")
    private boolean cookieSecure;

    @Operation(summary = "회원가입")
    @SecurityRequirements
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "로그인",
            description = "성공 시 body에 accessToken, Set-Cookie(httpOnly)에 refreshToken을 담습니다."
    )
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginSession session = userService.login(request);
        ResponseCookie refreshCookie = AuthCookieFactory.createRefreshCookie(
                session.refreshToken(),
                Duration.ofMillis(jwtProperties.getRefreshTokenExpiration()),
                cookieSecure
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(session.body());
    }

    @Operation(summary = "내 정보 조회", description = "Authorization: Bearer {accessToken} 필요")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "접근 거부",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<JwtPrincipal> me(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(principal);
    }

    @Operation(
            summary = "토큰 재발급",
            description = "httpOnly 쿠키의 Refresh Token으로 새 accessToken을 발급하고, 쿠키의 refresh를 교체합니다(RTR)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 refreshToken",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = AuthCookieFactory.REFRESH_COOKIE_NAME, required = false) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshSession session = userService.refresh(refreshToken);

        ResponseCookie refreshCookie = AuthCookieFactory.createRefreshCookie(
                session.refreshToken(),
                Duration.ofMillis(jwtProperties.getRefreshTokenExpiration()),
                cookieSecure
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(session.body());
    }

    @Operation(
            summary = "로그아웃",
            description = "Refresh Token을 Redis에서 삭제하고, 쿠키를 만료시킵니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "로그아웃 처리 완료")
    })
    @SecurityRequirements
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = AuthCookieFactory.REFRESH_COOKIE_NAME, required = false) String refreshToken
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            userService.logout(refreshToken);
        }

        ResponseCookie clear = AuthCookieFactory.clearRefreshCookie(cookieSecure);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clear.toString())
                .build();
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "비밀번호를 다시 확인하고 단어·퀴즈 데이터를 삭제한 뒤 계정을 탈퇴 상태로 전환합니다."
    )
    @PostMapping("/withdraw")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody WithdrawRequest request
    ) {
        userService.withdraw(principal.userId(), request);
        ResponseCookie clear = AuthCookieFactory.clearRefreshCookie(cookieSecure);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clear.toString())
                .build();
    }
}
