package com.myenglishvocab.server.ai.quota;

import com.myenglishvocab.server.ai.config.AiProperties;
import com.myenglishvocab.server.common.exception.BusinessException;
import com.myenglishvocab.server.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Profile("test")
@RequiredArgsConstructor
public class InMemoryAiUsageLimiter implements AiUsageLimiter {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final AiProperties aiProperties;
    private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();

    @Override
    public void consume(Long userId) {
        String key = key(userId);
        int count = counts.computeIfAbsent(key, ignored -> new AtomicInteger(0)).incrementAndGet();
        if (count > aiProperties.getDailyLimit()) {
            throw new BusinessException(ErrorCode.AI_DAILY_LIMIT_EXCEEDED, aiProperties.getDailyLimit());
        }
    }

    @Override
    public int getUsedCount(Long userId) {
        AtomicInteger count = counts.get(key(userId));
        return count == null ? 0 : count.get();
    }

    private String key(Long userId) {
        return userId + ":" + LocalDate.now(ZONE);
    }
}