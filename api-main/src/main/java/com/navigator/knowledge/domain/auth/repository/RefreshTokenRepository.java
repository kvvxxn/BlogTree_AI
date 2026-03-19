package com.navigator.knowledge.domain.auth.repository;

import java.util.Optional;

public interface RefreshTokenRepository {
    // 저장 (이메일, 토큰값)
    void save(String email, String refreshToken);

    // 조회
    Optional<String> findByEmail(String email);

    // 삭제 (로그아웃용)
    void deleteByEmail(String email);
}
