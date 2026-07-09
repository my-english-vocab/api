package com.myenglishvocab.server.user.dto;

public record SignupRequest(
        String username,
        String password,
        String displayName
) {
}
