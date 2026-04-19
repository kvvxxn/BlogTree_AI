package com.navigator.knowledge.domain.tree.controller;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeTreeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

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

    private Long userId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = userRepository.save(User.builder()
            .email("tree-controller@example.com")
            .name("Tree Controller User")
            .profileImageUrl("https://example.com/profile.png")
            .role(Role.USER)
            .careerGoal("Backend Developer")
            .build());

        userId = user.getId();
    }

    @Test
    @DisplayName("GET /api/tree는 category-topic-keyword 중첩 구조를 그대로 반환한다")
    void getKnowledgeTree_returnsNestedShape() throws Exception {
        when(knowledgeService.getKnowledgeTree(userId)).thenReturn(Map.of(
            "Backend", Map.of(
                "Database", List.of("PostgreSQL", "Redis"),
                "Infra", List.of("Docker")
            )
        ));

        mockMvc.perform(get("/api/tree")
                .header("Authorization", authorizationHeader(userId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.Backend.Database[0]").value("PostgreSQL"))
            .andExpect(jsonPath("$.Backend.Database[1]").value("Redis"))
            .andExpect(jsonPath("$.Backend.Infra[0]").value("Docker"));
    }

    private String authorizationHeader(Long id) {
        return "Bearer " + jwtProvider.createAccessToken(id, "ROLE_USER");
    }
}
