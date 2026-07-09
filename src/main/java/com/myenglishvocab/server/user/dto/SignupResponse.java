package com.myenglishvocab.server.user.dto;

import com.myenglishvocab.server.user.entity.User;

public record SignupResponse(
        Long id,
        String username,
        String displayName
) {
    // 응답을 만들 때 password를 빼기 위해 이렇게 만들어줌
    public static SignupResponse from(User user) {
        return new SignupResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName()
        );
    }
}
