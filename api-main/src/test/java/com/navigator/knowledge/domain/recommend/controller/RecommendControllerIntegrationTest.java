package com.navigator.knowledge.domain.recommend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navigator.knowledge.domain.recommend.entity.Recommendation;
import com.navigator.knowledge.domain.recommend.messaging.listener.RecommendTaskListener;
import com.navigator.knowledge.domain.recommend.messaging.producer.RecommendTaskProducer;
import com.navigator.knowledge.domain.recommend.repository.RecommendationRepository;
import com.navigator.knowledge.domain.task.entity.Task;
import com.navigator.knowledge.domain.task.entity.TaskStatus;
import com.navigator.knowledge.domain.task.entity.TaskType;
import com.navigator.knowledge.domain.task.repository.TaskRepository;
import com.navigator.knowledge.domain.task.sse.SseEmitterService;
import com.navigator.knowledge.domain.tree.service.KnowledgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.ai.openai.api-key=test-api-key",
    "spring.ai.openai.embedding.options.model=test-model",
    "oauth2.google.client-id=test-client-id",
    "oauth2.google.client-secret=test-client-secret",
    "jwt.secret=dGVzdC1qd3Qtc2VjcmV0LWZvci1pbnRlZ3JhdGlvbi10ZXN0cw==",
    "spring.autoconfigure.exclude=" +
        "org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.neo4j.Neo4jRepositoriesAutoConfiguration," +
        "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"
})
@AutoConfigureMockMvc
class RecommendControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @MockBean
    private RecommendTaskProducer recommendTaskProducer;

    @MockBean
    private RecommendTaskListener recommendTaskListener;

    @MockBean
    private ConnectionFactory connectionFactory;

    @MockBean
    private KnowledgeService knowledgeService;

    @MockBean
    private SseEmitterService sseEmitterService;

    @BeforeEach
    void setUp() {
        recommendationRepository.deleteAll();
        taskRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/recommend/{recommendationId}는 저장된 추천 결과를 반환한다")
    void getRecommendation_returnsPersistedRecommendation() throws Exception {
        Task task = taskRepository.save(Task.builder()
            .taskId("task-recommend-get-001")
            .userId(1L)
            .taskType(TaskType.RECOMMEND)
            .status(TaskStatus.SUCCESS)
            .expiresAt(LocalDateTime.now().plusSeconds(45))
            .build());

        Recommendation recommendation = recommendationRepository.save(Recommendation.builder()
            .task(task)
            .userId(1L)
            .reason("추천 이유")
            .category("Backend")
            .topic("Spring")
            .keyword("JPA")
            .build());

        mockMvc.perform(get("/api/recommend/{recommendationId}", recommendation.getRecommendationId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskId").value("task-recommend-get-001"))
            .andExpect(jsonPath("$.reason").value("추천 이유"))
            .andExpect(jsonPath("$.category").value("Backend"))
            .andExpect(jsonPath("$.topic").value("Spring"))
            .andExpect(jsonPath("$.keyword").value("JPA"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/recommend/{recommendationId}는 없는 추천 결과 조회 시 404를 반환한다")
    void getRecommendation_returns404WhenRecommendationDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/recommend/{recommendationId}", 99999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("RECOMMENDATION_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("해당 추천 결과를 찾을 수 없습니다. recommendationId=99999"))
            .andExpect(jsonPath("$.path").value("/api/recommend/99999"));
    }

    @Test
    @DisplayName("POST /api/recommend는 task를 생성하고 PROCESSING 상태로 전환한 뒤 요청을 발행한다")
    void requestRecommendation_createsTaskAndPublishesMessage() throws Exception {
        when(knowledgeService.getKnowledgeTree(1L)).thenReturn(Map.of(
            "Backend", Map.of("Spring", java.util.List.of("JPA", "Neo4j"))
        ));
        doNothing().when(recommendTaskProducer).sendTaskRequest(any());

        mockMvc.perform(post("/api/recommend")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of())))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.taskId").isNotEmpty());

        assertThat(taskRepository.findAll()).hasSize(1);

        Task savedTask = taskRepository.findAll().get(0);
        assertThat(savedTask.getSourceUrl()).isNull();
        assertThat(savedTask.getUserId()).isEqualTo(1L);
        assertThat(savedTask.getTaskType()).isEqualTo(TaskType.RECOMMEND);
        assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.PROCESSING);

        verify(knowledgeService).getKnowledgeTree(1L);
        verify(recommendTaskProducer).sendTaskRequest(any());
    }
}
