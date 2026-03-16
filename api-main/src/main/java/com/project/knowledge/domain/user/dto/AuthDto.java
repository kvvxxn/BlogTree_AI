package com.project.knowledge.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class AuthDto {
    // 1. FE가 BE로 보낼 때 쓰는 상자
    // 인가 코드를 전달
    @Getter
    @NoArgsConstructor
    public static class LoginRequest {
        private String authorizationCode; //인가 코드 (프론트가 보냄)
    }

    // 2. BE가 FE로 보낼 때 쓰는 상자
    // 자체 토큰 발급 & 반환
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResponse {
        private String message;
        private String accessToken;
        // 두 토큰 모두 사용자에게 넘겨주되 refresh는 좀 더 보안을 깐깐하게
        // 그래야 refresh를 쓰는 의미가 있음
        private String refreshToken;
    }
}
