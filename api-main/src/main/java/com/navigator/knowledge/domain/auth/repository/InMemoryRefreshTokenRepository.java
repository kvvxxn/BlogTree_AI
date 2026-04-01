package com.navigator.knowledge.domain.auth.repository;

import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository // 스프링이 이 구현체를 빈(Bean)으로 등록.
public class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

    // 실질적인 메모리 저장소
    private final Map<String, String> tokenMap = new ConcurrentHashMap<>();

    @Override
    public void save(Long userId, String refreshToken) {
        tokenMap.put(String.valueOf(userId), refreshToken);
    }

    @Override
    public Optional<String> findByUserId(Long userId) {
        return Optional.ofNullable(tokenMap.get(String.valueOf(userId)));
    }

    @Override
    public void deleteByUserId(Long userId) {
        tokenMap.remove(String.valueOf(userId));
    }

    // HashMap 테스트용 메서드
    //public Map<String, String> getTokenMap() {
    //    return this.tokenMap;
    //}
}
