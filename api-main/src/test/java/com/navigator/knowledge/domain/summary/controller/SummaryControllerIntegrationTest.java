package com.navigator.knowledge.domain.summary.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navigator.knowledge.domain.summary.entity.Summary;
import com.navigator.knowledge.domain.summary.messaging.listener.SummaryTaskListener;
import com.navigator.knowledge.domain.summary.messaging.producer.SummaryTaskProducer;
import com.navigator.knowledge.domain.summary.repository.SummaryRepository;
import com.navigator.knowledge.domain.task.entity.Task;
import com.navigator.knowledge.domain.task.entity.TaskStatus;
import com.navigator.knowledge.domain.task.repository.TaskRepository;
import com.navigator.knowledge.domain.task.sse.SseEmitterService;
import com.navigator.knowledge.domain.tree.service.KnowledgeService;
import com.navigator.knowledge.global.infra.ai.TextEmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
class SummaryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private SummaryRepository summaryRepository;

    @MockBean
    private SummaryTaskProducer summaryTaskProducer;

    @MockBean
    private SummaryTaskListener summaryTaskListener;

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
        summaryRepository.deleteAll();
        taskRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/summary/{summaryId}는 저장된 요약을 반환한다")
    void getSummary_returnsPersistedSummary() throws Exception {
        Task task = taskRepository.save(Task.builder()
                .taskId("task-get-001")
                .userId(1L)
                .sourceUrl("https://example.com/article")
                .status(TaskStatus.SUCCESS)
                .build());

        Summary summary = summaryRepository.save(Summary.builder()
                .task(task)
                .userId(1L)
                .sourceUrl("https://example.com/article")
                .content("요약 본문")
                .build());

        mockMvc.perform(get("/api/summary/{summaryId}", summary.getSummaryId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceUrl").value("https://example.com/article"))
                .andExpect(jsonPath("$.content").value("요약 본문"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/summary/{summaryId}는 없는 요약 조회 시 404를 반환한다")
    void getSummary_returns404WhenSummaryDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/summary/{summaryId}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("SUMMARY_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("해당 요약을 찾을 수 없습니다. summaryId=99999"))
                .andExpect(jsonPath("$.path").value("/api/summary/99999"));
    }

    @Test
    @DisplayName("POST /api/summary는 task를 생성하고 PROCESSING 상태로 전환한 뒤 요청을 발행한다")
    void requestSummary_createsTaskAndPublishesMessage() throws Exception {
        when(knowledgeService.getKnowledgeTree(1L)).thenReturn(Map.of(
                "Backend", Map.of("Spring", java.util.List.of("JPA", "Neo4j"))
        ));
        doNothing().when(summaryTaskProducer).sendTaskRequest(any());

        mockMvc.perform(post("/api/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sourceUrl", "https://example.com/new-article"
                        ))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.taskId").isNotEmpty());

        assertThat(taskRepository.findAll()).hasSize(1);

        Task savedTask = taskRepository.findAll().get(0);
        assertThat(savedTask.getSourceUrl()).isEqualTo("https://example.com/new-article");
        assertThat(savedTask.getUserId()).isEqualTo(1L);
        assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.PROCESSING);

        verify(knowledgeService).getKnowledgeTree(1L);
        verify(summaryTaskProducer).sendTaskRequest(any());
    }

    @Test
    @DisplayName("POST /api/summary는 sourceUrl이 비어 있으면 400을 반환한다")
    void requestSummary_returns400WhenSourceUrlIsBlank() throws Exception {
        mockMvc.perform(post("/api/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sourceUrl", ""
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("sourceUrl: sourceUrl은 비어 있을 수 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/summary"));
    }
}
