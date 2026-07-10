package com.myenglishvocab.server.user.dto;

public record LoginResponse(
        Long userId,
        String username,
        String displayName,
        String message
) {
}
