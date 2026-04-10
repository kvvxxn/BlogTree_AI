package com.navigator.knowledge.domain.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final Map<Long, String> tokenMap = new ConcurrentHashMap<>();

    @Override
    public void save(Long userId, String refreshToken) {
        tokenMap.put(userId, refreshToken);
    }

    @Override
    public Optional<String> findByUserId(Long userId) {
        return Optional.ofNullable(tokenMap.get(userId));
    }

    @Override
    public void deleteByUserId(Long userId) {
        tokenMap.remove(userId);
    }
}
