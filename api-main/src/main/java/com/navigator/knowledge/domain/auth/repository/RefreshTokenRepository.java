package com.navigator.knowledge.domain.auth.repository;

import java.util.Optional;

public interface RefreshTokenRepository {
    // 저장 (아이디, 토큰값)
    void save(Long userId, String refreshToken);

    // 조회
    Optional<String> findByUserId(Long userId);

    // 삭제 (로그아웃용)
    void deleteByUserId(Long userId);
}
