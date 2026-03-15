package com.project.knowledge.domain.user.repository;

import com.project.knowledge.domain.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // 이메일로 해당 유저의 교환권(Refresh Token)을 찾는 기능
    Optional<RefreshToken> findByEmail(String email);

}
