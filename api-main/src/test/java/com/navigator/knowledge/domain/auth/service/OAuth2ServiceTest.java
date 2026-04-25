package com.navigator.knowledge.domain.auth.service;

import com.navigator.knowledge.domain.auth.dto.AuthDto;
import com.navigator.knowledge.domain.auth.repository.RefreshTokenRepository;
import com.navigator.knowledge.domain.user.entity.Role;
import com.navigator.knowledge.domain.user.entity.User;
import com.navigator.knowledge.domain.user.repository.UserRepository;
import com.navigator.knowledge.global.security.jwt.JwtProvider;
import com.navigator.knowledge.global.security.oauth2.GoogleAuthClient;
import com.navigator.knowledge.global.security.oauth2.dto.GoogleTokenResponse;
import com.navigator.knowledge.global.security.oauth2.dto.GoogleUserInfoDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuth2ServiceTest {

    @Mock
    private GoogleAuthClient googleAuthClient;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private OAuth2Service oAuth2Service;

    @Test
    @DisplayName("구글 첫 로그인 시 신규 사용자를 저장한 뒤 토큰을 발급한다")
    void googleLogin_savesNewUserAndIssuesTokens() {
        GoogleTokenResponse tokenResponse = new GoogleTokenResponse();
        GoogleUserInfoDto userInfo = createGoogleUserInfo("new-user@example.com", "New User");
        User savedUser = User.builder()
                .id(1L)
                .email("new-user@example.com")
                .name("New User")
                .profileImageUrl("https://example.com/new.png")
                .role(Role.USER)
                .build();

        given(googleAuthClient.getGoogleAccessToken("code", "https://example.com/callback")).willReturn(tokenResponse);
        given(googleAuthClient.getGoogleUserInfo(tokenResponse)).willReturn(userInfo);
        given(userRepository.findByEmail("new-user@example.com")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtProvider.createAccessToken(1L, Role.USER.getKey())).willReturn("access-token");
        given(jwtProvider.createRefreshToken(1L)).willReturn("refresh-token");

        AuthDto.LoginResponse response = oAuth2Service.googleLogin("code", "https://example.com/callback");

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenRepository).save(1L, "refresh-token");
    }

    @Test
    @DisplayName("기존 사용자가 다시 로그인하면 사용자 정보를 갱신하고 토큰을 저장한다")
    void googleLogin_updatesExistingUserAndStoresTokens() {
        GoogleTokenResponse tokenResponse = new GoogleTokenResponse();
        GoogleUserInfoDto userInfo = createGoogleUserInfo("existing-user@example.com", "Existing User");
        User existingUser = User.builder()
                .id(2L)
                .email("existing-user@example.com")
                .name("Before Update")
                .profileImageUrl("https://example.com/old.png")
                .role(Role.USER)
                .build();

        given(googleAuthClient.getGoogleAccessToken("code", null)).willReturn(tokenResponse);
        given(googleAuthClient.getGoogleUserInfo(tokenResponse)).willReturn(userInfo);
        given(userRepository.findByEmail("existing-user@example.com")).willReturn(Optional.of(existingUser));
        given(userRepository.save(existingUser)).willReturn(existingUser);
        given(jwtProvider.createAccessToken(2L, Role.USER.getKey())).willReturn("access-token");
        given(jwtProvider.createRefreshToken(2L)).willReturn("refresh-token");

        oAuth2Service.googleLogin("code", null);

        verify(refreshTokenRepository).save(2L, "refresh-token");
        assertThat(existingUser.getName()).isEqualTo("Existing User");
        assertThat(existingUser.getProfileImageUrl()).isEqualTo("https://example.com/new.png");
    }

    private GoogleUserInfoDto createGoogleUserInfo(String email, String name) {
        GoogleUserInfoDto userInfoDto = new GoogleUserInfoDto();
        setField(userInfoDto, "email", email);
        setField(userInfoDto, "name", name);
        setField(userInfoDto, "picture", "https://example.com/new.png");
        return userInfoDto;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
