package com.navigator.knowledge.domain.user.repository;

import com.navigator.knowledge.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 소셜 로그인 시 이미 가입된 사용자인지 이메일로 확인하기 위한 메서드
    Optional<User> findByEmail(String email);

}
