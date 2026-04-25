package com.navigator.knowledge.domain.auth.service;

import com.navigator.knowledge.domain.auth.dto.AuthDto;
import com.navigator.knowledge.domain.auth.dto.DevAuthTokenRequest;
import com.navigator.knowledge.domain.auth.repository.RefreshTokenRepository;
import com.navigator.knowledge.domain.user.entity.Role;
import com.navigator.knowledge.domain.user.entity.User;
import com.navigator.knowledge.domain.user.repository.UserRepository;
import com.navigator.knowledge.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DevAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private DevAuthService devAuthService;

    @Test
    @DisplayName("개발용 토큰 발급은 신규 사용자를 저장하고 access/refresh token을 반환한다")
    void issueDevToken_createsUserAndReturnsTokens() {
        DevAuthTokenRequest request = new DevAuthTokenRequest();
        User savedUser = User.builder()
            .id(1L)
            .email("swagger-dev@example.com")
            .name("Swagger Dev User")
            .profileImageUrl("https://example.com/swagger-dev.png")
            .role(Role.USER)
            .careerGoal("Backend Developer")
            .build();

        given(userRepository.findByEmail("swagger-dev@example.com")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtProvider.createAccessToken(1L, Role.USER.getKey())).willReturn("access-token");
        given(jwtProvider.createRefreshToken(1L)).willReturn("refresh-token");

        AuthDto.LoginResponse response = devAuthService.issueDevToken(request);

        assertThat(response.getMessage()).isEqualTo("개발용 토큰 발급 성공!");
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenRepository).save(1L, "refresh-token");
    }
}
