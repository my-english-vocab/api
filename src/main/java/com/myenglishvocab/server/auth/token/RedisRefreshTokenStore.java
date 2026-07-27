package com.myenglishvocab.server.auth.token;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String refreshToken, Long userId, Duration ttl) {
        redisTemplate.opsForValue().set(key(refreshToken), String.valueOf(userId), ttl);
    }

    @Override
    public Optional<Long> findUserId(String refreshToken) {
        String value = redisTemplate.opsForValue().get(key(refreshToken));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(Long.valueOf(value));
    }

    @Override
    public void delete(String refreshToken) {
        redisTemplate.delete(key(refreshToken));
    }

    private String key(String refreshToken) {
        return KEY_PREFIX + refreshToken;
    }
}
