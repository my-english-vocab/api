package com.myenglishvocab.server.auth.jwt;

public record JwtPrincipal(
        Long userId,
        String username
) {
}
