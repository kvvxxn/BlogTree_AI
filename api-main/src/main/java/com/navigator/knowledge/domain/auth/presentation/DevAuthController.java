package com.navigator.knowledge.domain.auth.presentation;

import com.navigator.knowledge.domain.auth.dto.AuthDto;
import com.navigator.knowledge.domain.auth.dto.DevAuthTokenRequest;
import com.navigator.knowledge.domain.auth.service.DevAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Dev Auth")
public class DevAuthController {

    private final DevAuthService devAuthService;

    @PostMapping("/dev-token")
    @Operation(
        summary = "개발용 JWT 발급",
        description = "dev profile에서만 활성화됩니다. Swagger UI 테스트용으로 access/refresh token을 발급합니다."
    )
    public ResponseEntity<AuthDto.LoginResponse> issueDevToken(@RequestBody(required = false) DevAuthTokenRequest request) {
        DevAuthTokenRequest actualRequest = request == null ? new DevAuthTokenRequest() : request;
        return ResponseEntity.ok(devAuthService.issueDevToken(actualRequest));
    }
}
