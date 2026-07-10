package com.myenglishvocab.server.user.dto;

public record LoginRequest(
        String username,
        String password
) {
}
