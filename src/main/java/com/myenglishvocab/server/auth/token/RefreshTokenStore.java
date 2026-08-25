package com.myenglishvocab.server.auth.token;

import java.time.Duration;
import java.util.Optional;

public interface RefreshTokenStore {

    void save(String refreshToken, Long userId, Duration ttl);

    void delete(String refreshToken);

    void deleteAllByUserId(Long userId);

    Optional<Long> findUserId(String refreshToken);
}
