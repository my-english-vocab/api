package com.myenglishvocab.server.admin.dto;

import com.myenglishvocab.server.analytics.entity.AccountLifecycleEvent;
import com.myenglishvocab.server.analytics.entity.AccountLifecycleType;

import java.time.Instant;

public record AccountLifecycleResponse(
        Long eventId,
        Long userId,
        String username,
        AccountLifecycleType eventType,
        Instant occurredAt
) {
    public static AccountLifecycleResponse from(AccountLifecycleEvent event) {
        Long userId = event.getUser() == null ? null : event.getUser().getId();
        return new AccountLifecycleResponse(
                event.getId(),
                userId,
                event.getUsernameSnapshot(),
                event.getEventType(),
                event.getOccurredAt()
        );
    }
}
