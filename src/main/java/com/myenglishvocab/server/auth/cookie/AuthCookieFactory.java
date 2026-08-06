package com.myenglishvocab.server.auth.cookie;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

public class AuthCookieFactory {

    public static final String REFRESH_COOKIE_NAME = "refresh_token";

    private AuthCookieFactory() {
    }

    public static ResponseCookie createRefreshCookie(String refreshToken, Duration maxAge, boolean secure) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .path("/api/auth")
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();
    }

    public static ResponseCookie clearRefreshCookie(boolean secure) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .path("/api/auth")
                .sameSite("Lax")
                .maxAge(0)
                .build();
    }
}
