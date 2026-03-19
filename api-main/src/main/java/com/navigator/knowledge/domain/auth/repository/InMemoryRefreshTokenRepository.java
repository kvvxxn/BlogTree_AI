package com.navigator.knowledge.domain.auth.repository;

import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository // 스프링이 이 구현체를 빈(Bean)으로 등록합니다.
public class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

    // 실질적인 메모리 저장소
    private final Map<String, String> tokenMap = new ConcurrentHashMap<>();

    @Override
    public void save(String email, String refreshToken) {
        tokenMap.put(email, refreshToken);
    }

    @Override
    public Optional<String> findByEmail(String email) {
        return Optional.ofNullable(tokenMap.get(email));
    }

    @Override
    public void deleteByEmail(String email) {
        tokenMap.remove(email);
    }

    // HashMap 테스트용 메서드
    public Map<String, String> getTokenMap() {
        return this.tokenMap;
    }
}
