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
    public ResponseEntity<AuthDto.TokenResponse> googleLogin(@RequestBody AuthDto.LoginRequest request) {
        String authcode = request.getAuthorizationCode();
        String redirectUri = request.getRedirectUri();

        AuthDto.LoginResponse response = oAuth2Service.googleLogin(authcode, redirectUri);
        
        // refresh token은 HttpOnly 쿠키로만 전달하고, 응답 본문에는 access token만 담는다.
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.createRefreshTokenCookie(response.getRefreshToken()))
                .body(new AuthDto.TokenResponse(response.getMessage(), response.getAccessToken()));
    }

    // HashMap 테스트용 메서드
    //@GetMapping("/test/refresh-tokens")
    //public ResponseEntity<Map<String, String>> checkTokens() {
    //    return ResponseEntity.ok(oAuth2Service.getAllTokensForTest());
    //}

    // --- [토큰 재발급 API] ---
    @PostMapping("/reissue")
    public ResponseEntity<AuthDto.TokenResponse> reissue(
            @RequestHeader(value = "Refresh-Token", required = false) String refreshTokenHeader,
            HttpServletRequest request
    ) {
        String refreshToken = resolveRefreshToken(refreshTokenHeader, request);
        AuthDto.LoginResponse response = oAuth2Service.reissue(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.createRefreshTokenCookie(response.getRefreshToken()))
                .body(new AuthDto.TokenResponse(response.getMessage(), response.getAccessToken()));
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
