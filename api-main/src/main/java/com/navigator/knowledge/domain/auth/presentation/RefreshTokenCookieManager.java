package com.navigator.knowledge.domain.auth.presentation;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
public class RefreshTokenCookieManager {

    private final String cookieName;
    private final boolean secure;
    private final String sameSite;
    private final String path;
    private final String domain;
    private final Duration maxAge;

    public RefreshTokenCookieManager(
            @Value("${app.auth.refresh-cookie.name:refreshToken}") String cookieName,
            @Value("${app.auth.refresh-cookie.secure:false}") boolean secure,
            @Value("${app.auth.refresh-cookie.same-site:Lax}") String sameSite,
            @Value("${app.auth.refresh-cookie.path:/api/auth}") String path,
            @Value("${app.auth.refresh-cookie.domain:}") String domain,
            @Value("${jwt.refresh-expiration}") long refreshTokenExpirationMs
    ) {
        this.cookieName = cookieName;
        this.secure = secure;
        this.sameSite = sameSite;
        this.path = path;
        this.domain = domain;
        this.maxAge = Duration.ofMillis(refreshTokenExpirationMs);
    }

    public String createRefreshTokenCookie(String refreshToken) {
        return buildCookie(refreshToken, maxAge).toString();
    }

    public String clearRefreshTokenCookie() {
        return buildCookie("", Duration.ZERO).toString();
    }

    public String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private ResponseCookie buildCookie(String value, Duration cookieMaxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .sameSite(sameSite)
                .maxAge(cookieMaxAge);

        if (StringUtils.hasText(domain)) {
            builder.domain(domain);
        }

        return builder.build();
    }
}
