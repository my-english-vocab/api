package com.myenglishvocab.server.user.dto;

import com.myenglishvocab.server.user.entity.User;

public record ProfileResponse(
        Long userId,
        String username,
        String displayName
) {
    public static ProfileResponse from(User user) {
        return new ProfileResponse(user.getId(), user.getUsername(), user.getDisplayName());
    }
}
