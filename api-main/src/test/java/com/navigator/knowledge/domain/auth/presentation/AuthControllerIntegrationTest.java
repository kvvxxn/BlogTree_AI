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
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
    @DisplayName("POST /api/auth/google은 사용자 저장 후 refresh token을 저장하고 설정된 만료시간으로 발급한다")
    void googleLogin_persistsUserAndRefreshTokenWithConfiguredExpiration() throws Exception {
        GoogleTokenResponse googleTokenResponse = createGoogleTokenResponse();
        when(googleAuthClient.getGoogleAccessToken("google-auth-code", null))
                .thenReturn(googleTokenResponse);
        when(googleAuthClient.getGoogleUserInfo(googleTokenResponse))
                .thenReturn(createGoogleUserInfo("user@example.com", "Tester"));

        String responseBody = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "authorizationCode", "google-auth-code"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("구글 로그인 및 토큰 발급 성공!"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, String> tokenResponse = objectMapper.readValue(responseBody, objectMapper.getTypeFactory()
                .constructMapType(Map.class, String.class, String.class));

        User savedUser = userRepository.findByEmail("user@example.com").orElseThrow();
        String savedRefreshToken = refreshTokenRepository.findByUserId(savedUser.getId()).orElseThrow();
        Claims refreshClaims = parseClaims(savedRefreshToken);

        assertThat(savedRefreshToken).isEqualTo(tokenResponse.get("refreshToken"));
        assertThat(refreshClaims.getSubject()).isEqualTo(String.valueOf(savedUser.getId()));
        assertThat(refreshClaims.getExpiration().getTime() - refreshClaims.getIssuedAt().getTime())
                .isEqualTo(REFRESH_EXPIRATION);
    }

    @Test
    @DisplayName("POST /api/auth/reissue는 저장된 refresh token으로 access token과 refresh token을 재발급한다")
    void reissue_rotatesStoredRefreshToken() throws Exception {
        User user = userRepository.save(User.builder()
                .email("reissue@example.com")
                .name("Reissue User")
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
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        String storedRefreshToken = refreshTokenRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(storedRefreshToken).isNotBlank();
        assertThat(parseClaims(storedRefreshToken).getSubject()).isEqualTo(String.valueOf(user.getId()));
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
    @DisplayName("POST /api/auth/logout은 인증된 사용자의 refresh token을 삭제한다")
    void logout_deletesRefreshToken() throws Exception {
        User user = userRepository.save(User.builder()
                .email("logout@example.com")
                .name("Logout User")
                .profileImageUrl("https://example.com/profile.png")
                .role(Role.USER)
                .build());
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().getKey());
        refreshTokenRepository.save(user.getId(), jwtProvider.createRefreshToken(user.getId()));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("성공적으로 로그아웃 되었습니다."));

        assertThat(refreshTokenRepository.findByUserId(user.getId())).isEmpty();
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
