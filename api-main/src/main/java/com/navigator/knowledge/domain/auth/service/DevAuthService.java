package com.navigator.knowledge.domain.auth.service;

import com.navigator.knowledge.domain.auth.dto.AuthDto;
import com.navigator.knowledge.domain.auth.dto.DevAuthTokenRequest;
import com.navigator.knowledge.domain.auth.repository.RefreshTokenRepository;
import com.navigator.knowledge.domain.user.entity.Role;
import com.navigator.knowledge.domain.user.entity.User;
import com.navigator.knowledge.domain.user.repository.UserRepository;
import com.navigator.knowledge.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class DevAuthService {

    private static final String DEFAULT_EMAIL = "swagger-dev@example.com";
    private static final String DEFAULT_NAME = "Swagger Dev User";
    private static final String DEFAULT_CAREER_GOAL = "Backend Developer";
    private static final String DEFAULT_PROFILE_IMAGE_URL = "https://example.com/swagger-dev.png";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    public AuthDto.LoginResponse issueDevToken(DevAuthTokenRequest request) {
        String email = normalize(request.getEmail(), DEFAULT_EMAIL);
        String name = normalize(request.getName(), DEFAULT_NAME);
        String careerGoal = normalize(request.getCareerGoal(), DEFAULT_CAREER_GOAL);

        User user = userRepository.findByEmail(email)
            .map(existingUser -> {
                existingUser.update(name, DEFAULT_PROFILE_IMAGE_URL);
                existingUser.updateCareerGoal(careerGoal);
                return userRepository.save(existingUser);
            })
            .orElseGet(() -> userRepository.save(User.builder()
                .email(email)
                .name(name)
                .profileImageUrl(DEFAULT_PROFILE_IMAGE_URL)
                .role(Role.USER)
                .careerGoal(careerGoal)
                .build()));

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().getKey());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId(), refreshToken);

        return new AuthDto.LoginResponse(
            "개발용 토큰 발급 성공!",
            accessToken,
            refreshToken
        );
    }

    private String normalize(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
