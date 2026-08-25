package com.myenglishvocab.server.auth.jwt;

import com.myenglishvocab.server.user.entity.UserRole;

public record JwtPrincipal(
        Long userId,
        String username,
        UserRole role
) {
}
