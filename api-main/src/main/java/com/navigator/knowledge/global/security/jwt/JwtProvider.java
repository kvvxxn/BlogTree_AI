package com.navigator.knowledge.global.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    // 1. yml 파일에 숨겨둔 비밀키와 만료시간을 가져옴
    public JwtProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-expiration}") long refreshTokenExpiration) {
        // 문자열로 된 비밀키를 컴퓨터가 좋아하는 Byte 배열로 바꿔서 진짜 암호화 키로
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    // 2. Access Token 발급 버튼
    public String createAccessToken(Long userId, String role) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + this.accessTokenExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId)) // 이 토큰 주인의 ID
                .claim("role", role) // 이 사람의 권한(USER, ADMIN 등)도 토큰에 적어둔다.
                .issuedAt(now) // 토큰 발급 시간
                .expiration(validity) // 토큰 만료 시간
                .signWith(key) // 이게 없으면 위조된 가짜 토큰
                .compact(); // 토큰 압축해서 문자열로 반환
    }

    // 3. Refresh Token 발급 버튼
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now) // 발급 시간
                .expiration(validity) // 만료 시간 (7일 뒤)
                .signWith(key)
                .compact();
    }

    // 4. API 요청 시 토큰 진위 판별 로직
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true; // 무사통과! 정상 토큰

        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다. 재발급이 필요합니다.");
            throw new IllegalArgumentException("EXPIRED_TOKEN"); // 프론트에게 만료를 알림

        } catch (SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명입니다. 위조가 의심됩니다.");
            throw new IllegalArgumentException("INVALID_TOKEN"); // 프론트에게 위조를 알림

        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.");
            throw new IllegalArgumentException("INVALID_TOKEN");

        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 잘못되었습니다. (비어있음 등)");
            throw new IllegalArgumentException("INVALID_TOKEN");
        }
    }

    // 5. 정상 토큰이라면 UserId 꺼내기
    public Long getUserIdFromToken(String token) {
        String subject = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject(); // 토큰 만들 때 subject에 userId를 넣었으니 여기서 꺼냅니다.

        // subject는 무조건 String으로 나오니까, Long 타입으로 변환해서 리턴!
        return Long.parseLong(subject);
    }

    // 6. 정상 토큰이라면 권한(Role) 꺼내기
    public String getRoleFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class); // "role"이라는 이름의 Claim을 String 타입으로 꺼냅니다.
    }

    // 위조 여부 검증 시 추출할 메서드
    public String getEmailFromToken(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject(); // 토큰 만들 때 subject에 email을 넣었으니 여기서 꺼냅니다.
    }
}
