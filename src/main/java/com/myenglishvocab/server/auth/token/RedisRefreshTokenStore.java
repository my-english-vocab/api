package com.myenglishvocab.server.auth.token;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.Optional;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";
    private static final String USER_KEY_PREFIX = "refresh-user:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String refreshToken, Long userId, Duration ttl) {
        redisTemplate.opsForValue().set(key(refreshToken), String.valueOf(userId), ttl);
        String userKey = userKey(userId);
        redisTemplate.opsForSet().add(userKey, refreshToken);
        redisTemplate.expire(userKey, ttl);
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
        findUserId(refreshToken).ifPresent(userId ->
                redisTemplate.opsForSet().remove(userKey(userId), refreshToken));
        redisTemplate.delete(key(refreshToken));
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        String userKey = userKey(userId);
        Set<String> tokens = redisTemplate.opsForSet().members(userKey);
        if (tokens != null && !tokens.isEmpty()) {
            redisTemplate.delete(tokens.stream().map(this::key).toList());
        }
        redisTemplate.delete(userKey);
    }

    private String key(String refreshToken) {
        return KEY_PREFIX + refreshToken;
    }

    private String userKey(Long userId) {
        return USER_KEY_PREFIX + userId;
    }
}
