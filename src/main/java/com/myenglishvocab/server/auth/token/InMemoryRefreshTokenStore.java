package com.myenglishvocab.server.auth.token;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("test")
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private final Map<String, Long> store = new ConcurrentHashMap<>();

    @Override
    public void save(String refreshToken, Long userId, Duration ttl) {
        store.put(refreshToken, userId);
    }

    @Override
    public Optional<Long> findUserId(String refreshToken) {
        return Optional.ofNullable(store.get(refreshToken));
    }

    @Override
    public void delete(String refreshToken) {
        store.remove(refreshToken);
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        store.entrySet().removeIf(entry -> entry.getValue().equals(userId));
    }
}
