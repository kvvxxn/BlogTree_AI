package com.navigator.knowledge.domain.auth.presentation;

import com.navigator.knowledge.domain.auth.dto.AuthDto;
import com.navigator.knowledge.domain.auth.service.OAuth2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final OAuth2Service oAuth2Service;

    // 로그인
    @PostMapping("/google")
    public ResponseEntity<AuthDto.LoginResponse> googleLogin(@RequestBody AuthDto.LoginRequest request) {
        String authcode = request.getAuthorizationCode();
        String redirectUri = request.getRedirectUri();

        AuthDto.LoginResponse response = oAuth2Service.googleLogin(authcode, redirectUri);
        
        // 서비스가 만들어온 토큰(access, refresh)을 200 ok 상태코드와 함께 프론트에 던져줌
        return ResponseEntity.ok(response);
    }

    // HashMap 테스트용 메서드
    //@GetMapping("/test/refresh-tokens")
    //public ResponseEntity<Map<String, String>> checkTokens() {
    //    return ResponseEntity.ok(oAuth2Service.getAllTokensForTest());
    //}

    // --- [토큰 재발급 API] ---
    @PostMapping("/reissue")
    public ResponseEntity<AuthDto.LoginResponse> reissue(@RequestHeader("Refresh-Token") String refreshToken) {
        // 프론트가 HTTP Header에 'Refresh-Token'이라는 이름으로 토큰을 담아 보냅니다.
        AuthDto.LoginResponse response = oAuth2Service.reissue(refreshToken);
        return ResponseEntity.ok(response);
    }

    // --- [로그아웃 API] ---
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal Long userId) {
        oAuth2Service.logout(userId);

        return ResponseEntity.ok("성공적으로 로그아웃 되었습니다.");
    }
}