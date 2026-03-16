package com.project.knowledge.global.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final long accessTokenExpiration;

    // 1. yml 파일에 숨겨둔 비밀키와 만료시간을 가져와서 공장 세팅!
    public JwtProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-expiration}") long accessTokenExpiration) {
        // 문자열로 된 비밀키를 컴퓨터가 좋아하는 Byte 배열로 바꿔서 진짜 암호화 키로 만듭니다.
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = accessTokenExpiration;
    }

    // 2. 사원증(Access Token) 발급 버튼
    public String createAccessToken(String email, String role) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + this.accessTokenExpiration);

        return Jwts.builder()
                .subject(email) // 이 토큰의 주인은 이 이메일이다!
                .claim("role", role) // 이 사람의 권한(USER, ADMIN 등)도 토큰에 적어둔다.
                .issuedAt(now) // 토큰 발급 시간
                .expiration(validity) // 토큰 만료 시간
                .signWith(key) // 우리 서버의 비밀키로 도장 쾅! (이게 없으면 위조된 가짜 토큰입니다)
                .compact(); // 토큰 압축해서 문자열로 반환
    }

    // 3. Refresh Token 발급 버튼
    private final long REFRESH_TOKEN_VALIDITY = 1000L * 60 * 60 * 24 * 7;

    public String createRefreshToken(String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + REFRESH_TOKEN_VALIDITY);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now) // 발급 시간
                .expiration(validity) // 만료 시간 (7일 뒤)
                .signWith(key) // 똑같은 비밀키로 암호화 도장 쾅!
                .compact();
    }
    
    // 4. API 요청 시 토큰 진위 판별 로직
}
