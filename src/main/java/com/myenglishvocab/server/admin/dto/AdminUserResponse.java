package com.myenglishvocab.server.admin.dto;

import com.myenglishvocab.server.user.entity.User;
import com.myenglishvocab.server.user.entity.UserRole;
import com.myenglishvocab.server.user.entity.UserStatus;

import java.time.Instant;

public record AdminUserResponse(
        Long userId,
        String username,
        String displayName,
        UserRole role,
        UserStatus status,
        Instant createdAt,
        Instant lastLoginAt,
        Instant lastActiveAt,
        Instant withdrawnAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                user.getLastActiveAt(),
                user.getWithdrawnAt()
        );
    }
}
