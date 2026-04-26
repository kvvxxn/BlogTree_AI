package com.navigator.knowledge.domain.auth.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenCookieManagerTest {

    @Test
    @DisplayName("운영 기본 refresh token 쿠키는 cross-site 요청을 위해 SameSite=None과 Secure를 사용한다")
    void createRefreshTokenCookie_usesCrossSiteSecureAttributes() {
        RefreshTokenCookieManager cookieManager = new RefreshTokenCookieManager(
                "refreshToken",
                true,
                "None",
                "/api/auth",
                "",
                1_800_000L
        );

        String cookie = cookieManager.createRefreshTokenCookie("refresh-token");

        assertThat(cookie)
                .contains("refreshToken=refresh-token")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("Path=/api/auth")
                .contains("SameSite=None");
    }
}
