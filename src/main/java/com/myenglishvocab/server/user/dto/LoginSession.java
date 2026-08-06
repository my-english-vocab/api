package com.myenglishvocab.server.user.dto;

public record LoginSession(
        LoginResponse body,
        String refreshToken
) {
}
