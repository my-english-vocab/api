package com.myenglishvocab.server.ai.quota;

import com.myenglishvocab.server.ai.config.AiProperties;
import com.myenglishvocab.server.common.exception.BusinessException;
import com.myenglishvocab.server.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class RedisAiUsageLimiter implements AiUsageLimiter {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final StringRedisTemplate redisTemplate;
    private final AiProperties aiProperties;

    @Override
    public void consume(Long userId) {
        String key = key(userId);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
        if (count == 1L) {
            redisTemplate.expire(key, ttlUntilEndOfDay());
        }
        if (count > aiProperties.getDailyLimit()) {
            log.warn("AI 일일 한도 초과 userId={} count={}", userId, count);
            throw new BusinessException(ErrorCode.AI_DAILY_LIMIT_EXCEEDED, aiProperties.getDailyLimit());
        }
    }

    @Override
    public int getUsedCount(Long userId) {
        String value = redisTemplate.opsForValue().get(key(userId));
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    private String key(Long userId) {
        return "ai:daily:" + userId + ":" + LocalDate.now(ZONE);
    }

    private Duration ttlUntilEndOfDay() {
        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDateTime endOfDay = now.toLocalDate().plusDays(1).atStartOfDay();
        Duration ttl = Duration.between(now, endOfDay);
        return ttl.isNegative() || ttl.isZero() ? Duration.ofDays(1) : ttl;
    }
}
