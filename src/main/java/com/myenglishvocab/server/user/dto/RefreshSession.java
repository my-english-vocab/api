package com.myenglishvocab.server.user.dto;

public record RefreshSession(
        TokenResponse body,
        String refreshToken
) {
}
