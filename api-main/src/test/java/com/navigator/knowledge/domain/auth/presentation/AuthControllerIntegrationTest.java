package com.navigator.knowledge.domain.auth.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navigator.knowledge.domain.auth.repository.RefreshTokenRepository;
import com.navigator.knowledge.domain.recommend.messaging.listener.RecommendTaskListener;
import com.navigator.knowledge.domain.recommend.messaging.producer.RecommendTaskProducer;
import com.navigator.knowledge.domain.summary.messaging.listener.SummaryTaskListener;
import com.navigator.knowledge.domain.summary.messaging.producer.SummaryTaskProducer;
import com.navigator.knowledge.domain.task.sse.SseEmitterService;
import com.navigator.knowledge.domain.tree.service.KnowledgeService;
import com.navigator.knowledge.domain.user.entity.Role;
import com.navigator.knowledge.domain.user.entity.User;
import com.navigator.knowledge.domain.user.repository.UserRepository;
import com.navigator.knowledge.global.infra.ai.TextEmbeddingService;
import com.navigator.knowledge.global.security.jwt.JwtProvider;
import com.navigator.knowledge.global.security.oauth2.GoogleAuthClient;
import com.navigator.knowledge.global.security.oauth2.dto.GoogleTokenResponse;
import com.navigator.knowledge.global.security.oauth2.dto.GoogleUserInfoDto;
import com.navigator.knowledge.global.security.oauth2.exception.GoogleOAuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    private static final String TEST_SECRET = "dGVzdC1qd3Qtc2VjcmV0LWZvci1pbnRlZ3JhdGlvbi10ZXN0cw==";
    private static final long REFRESH_EXPIRATION = 1_800_000L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @MockBean
    private GoogleAuthClient googleAuthClient;

    @MockBean
    private SummaryTaskProducer summaryTaskProducer;

    @MockBean
    private SummaryTaskListener summaryTaskListener;

    @MockBean
    private RecommendTaskProducer recommendTaskProducer;

    @MockBean
    private RecommendTaskListener recommendTaskListener;

    @MockBean
    private ConnectionFactory connectionFactory;

    @MockBean
    private KnowledgeService knowledgeService;

    @MockBean
    private TextEmbeddingService textEmbeddingService;

    @MockBean
    private SseEmitterService sseEmitterService;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteByUserId(1L);
        refreshTokenRepository.deleteByUserId(2L);
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/auth/google은 사용자 저장 후 refresh token을 쿠키와 저장소에 반영한다")
    void googleLogin_persistsUserAndRefreshTokenWithCookie() throws Exception {
        GoogleTokenResponse googleTokenResponse = createGoogleTokenResponse();
        when(googleAuthClient.getGoogleAccessToken("google-auth-code", null))
                .thenReturn(googleTokenResponse);
        when(googleAuthClient.getGoogleUserInfo(googleTokenResponse))
                .thenReturn(createGoogleUserInfo("user@example.com", "Tester"));

        MvcResult result = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "authorizationCode", "google-auth-code"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("구글 로그인 및 토큰 발급 성공!"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);

        Map<String, String> tokenResponse = objectMapper.readValue(responseBody, objectMapper.getTypeFactory()
                .constructMapType(Map.class, String.class, String.class));

        User savedUser = userRepository.findByEmail("user@example.com").orElseThrow();
        String savedRefreshToken = refreshTokenRepository.findByUserId(savedUser.getId()).orElseThrow();
        Claims refreshClaims = parseClaims(savedRefreshToken);

        assertThat(tokenResponse).doesNotContainKey("refreshToken");
        assertThat(setCookieHeader).contains("refreshToken=" + savedRefreshToken);
        assertThat(setCookieHeader).contains("HttpOnly");
        assertThat(setCookieHeader).contains("Path=/api/auth");
        assertThat(refreshClaims.getSubject()).isEqualTo(String.valueOf(savedUser.getId()));
        assertThat(refreshClaims.getExpiration().getTime() - refreshClaims.getIssuedAt().getTime())
                .isEqualTo(REFRESH_EXPIRATION);
    }

    @Test
    @DisplayName("POST /api/auth/google은 Google OAuth 실패를 400 응답으로 반환한다")
    void googleLogin_returnsBadRequestWhenGoogleOAuthFails() throws Exception {
        when(googleAuthClient.getGoogleAccessToken("used-google-auth-code", null))
                .thenThrow(new GoogleOAuthException("Google 로그인 요청이 만료되었거나 이미 사용되었습니다. 다시 로그인해 주세요."));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "authorizationCode", "used-google-auth-code"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GOOGLE_OAUTH_FAILED"))
                .andExpect(jsonPath("$.message").value("Google 로그인 요청이 만료되었거나 이미 사용되었습니다. 다시 로그인해 주세요."))
                .andExpect(jsonPath("$.path").value("/api/auth/google"));
    }

    @Test
    @DisplayName("POST /api/auth/reissue는 refresh token 쿠키로 access token과 refresh token을 재발급한다")
    void reissue_rotatesStoredRefreshTokenFromCookie() throws Exception {
        User user = userRepository.save(User.builder()
                .email("reissue@example.com")
                .name("Reissue User")
                .profileImageUrl("https://example.com/profile.png")
                .role(Role.USER)
                .build());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId(), refreshToken);

        MvcResult result = mockMvc.perform(post("/api/auth/reissue")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("토큰 재발급 성공!"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        Map<String, String> tokenResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class)
        );
        String storedRefreshToken = refreshTokenRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(tokenResponse).doesNotContainKey("refreshToken");
        assertThat(storedRefreshToken).isNotBlank();
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains("refreshToken=" + storedRefreshToken);
        assertThat(parseClaims(storedRefreshToken).getSubject()).isEqualTo(String.valueOf(user.getId()));
    }

    @Test
    @DisplayName("POST /api/auth/reissue는 기존 Refresh-Token 헤더도 계속 지원한다")
    void reissue_acceptsLegacyRefreshTokenHeader() throws Exception {
        User user = userRepository.save(User.builder()
                .email("legacy-reissue@example.com")
                .name("Legacy Reissue User")
                .profileImageUrl("https://example.com/profile.png")
                .role(Role.USER)
                .build());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId(), refreshToken);

        mockMvc.perform(post("/api/auth/reissue")
                        .header("Refresh-Token", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("토큰 재발급 성공!"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/logout은 인증 없이 호출하면 401을 반환한다")
    void logout_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("POST /api/auth/logout은 인증된 사용자의 refresh token을 삭제하고 쿠키를 만료시킨다")
    void logout_deletesRefreshTokenAndExpiresCookie() throws Exception {
        User user = userRepository.save(User.builder()
                .email("logout@example.com")
                .name("Logout User")
                .profileImageUrl("https://example.com/profile.png")
                .role(Role.USER)
                .build());
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().getKey());
        refreshTokenRepository.save(user.getId(), jwtProvider.createRefreshToken(user.getId()));

        MvcResult result = mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("성공적으로 로그아웃 되었습니다."))
                .andReturn();

        assertThat(refreshTokenRepository.findByUserId(user.getId())).isEmpty();
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains("refreshToken=")
                .contains("Max-Age=0");
    }

    private GoogleTokenResponse createGoogleTokenResponse() {
        return objectMapper.convertValue(Map.of(
                "access_token", "google-access-token",
                "expires_in", 3600,
                "scope", "email profile",
                "token_type", "Bearer",
                "id_token", "google-id-token"
        ), GoogleTokenResponse.class);
    }

    private GoogleUserInfoDto createGoogleUserInfo(String email, String name) {
        return objectMapper.convertValue(Map.of(
                "email", email,
                "name", name,
                "picture", "https://example.com/profile.png"
        ), GoogleUserInfoDto.class);
    }

    private Claims parseClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
