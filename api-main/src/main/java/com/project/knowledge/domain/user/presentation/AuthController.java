package com.project.knowledge.domain.user.presentation;

import com.project.knowledge.domain.user.dto.AuthDto;
import com.project.knowledge.domain.user.service.OAuth2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final OAuth2Service oAuth2Service;

    @PostMapping("/google")
    public ResponseEntity<AuthDto.LoginResponse> googleLogin(@RequestBody AuthDto.LoginRequest request) {
        String authcode = request.getAuthorizationCode();

        AuthDto.LoginResponse response = oAuth2Service.googleLogin(authcode);
        
        // 서비스가 만들어온 토큰(access, refresh)을 200 ok 상태코드와 함께 프론트에 던져줌
        return ResponseEntity.ok(response);
    }
}
