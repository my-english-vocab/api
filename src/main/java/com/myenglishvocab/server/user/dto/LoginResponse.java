package com.myenglishvocab.server.user.dto;

import com.myenglishvocab.server.user.entity.UserRole;

public record LoginResponse(
        Long userId,
        String username,
        String displayName,
        UserRole role,
        String accessToken,
        String tokenType
) {
}
