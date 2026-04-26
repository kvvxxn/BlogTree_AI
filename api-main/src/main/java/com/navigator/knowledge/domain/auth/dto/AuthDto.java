package com.navigator.knowledge.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthDto {
    // 1. FE가 BE로 보낼 때 쓰는 상자
    // 인가 코드를 전달
    @Getter
    @NoArgsConstructor
    public static class LoginRequest {
        private String authorizationCode; //인가 코드 (프론트가 보냄)
        private String redirectUri; // 인가 코드 발급 시 사용한 redirect_uri
    }

    // 2. BE가 FE로 보낼 때 쓰는 상자
    // 자체 토큰 발급 & 반환
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResponse {
        private String message;
        private String accessToken;
        private String refreshToken;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenResponse {
        private String message;
        private String accessToken;
    }
}
