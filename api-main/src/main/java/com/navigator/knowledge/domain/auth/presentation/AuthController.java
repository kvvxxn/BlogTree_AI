package com.navigator.knowledge.domain.auth.presentation;

import com.navigator.knowledge.domain.auth.dto.AuthDto;
import com.navigator.knowledge.domain.auth.service.OAuth2Service;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final OAuth2Service oAuth2Service;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    // 로그인
    @PostMapping("/google")
    public ResponseEntity<AuthDto.LoginResponse> googleLogin(@RequestBody AuthDto.LoginRequest request) {
        String authcode = request.getAuthorizationCode();
        String redirectUri = request.getRedirectUri();

        AuthDto.LoginResponse response = oAuth2Service.googleLogin(authcode, redirectUri);
        
        // 서비스가 만들어온 토큰(access, refresh)을 200 ok 상태코드와 함께 프론트에 던져줌
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.createRefreshTokenCookie(response.getRefreshToken()))
                .body(response);
    }

    // HashMap 테스트용 메서드
    //@GetMapping("/test/refresh-tokens")
    //public ResponseEntity<Map<String, String>> checkTokens() {
    //    return ResponseEntity.ok(oAuth2Service.getAllTokensForTest());
    //}

    // --- [토큰 재발급 API] ---
    @PostMapping("/reissue")
    public ResponseEntity<AuthDto.LoginResponse> reissue(
            @RequestHeader(value = "Refresh-Token", required = false) String refreshTokenHeader,
            HttpServletRequest request
    ) {
        String refreshToken = resolveRefreshToken(refreshTokenHeader, request);
        AuthDto.LoginResponse response = oAuth2Service.reissue(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.createRefreshTokenCookie(response.getRefreshToken()))
                .body(response);
    }

    // --- [로그아웃 API] ---
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal Long userId) {
        oAuth2Service.logout(userId);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.clearRefreshTokenCookie())
                .body("성공적으로 로그아웃 되었습니다.");
    }

    private String resolveRefreshToken(String refreshTokenHeader, HttpServletRequest request) {
        String refreshTokenFromCookie = refreshTokenCookieManager.extractRefreshToken(request);
        if (StringUtils.hasText(refreshTokenFromCookie)) {
            return refreshTokenFromCookie;
        }

        if (StringUtils.hasText(refreshTokenHeader)) {
            return refreshTokenHeader;
        }

        throw new IllegalArgumentException("refresh token이 없습니다.");
    }
}
