package com.navigator.knowledge.domain.task.controller;

import com.navigator.knowledge.domain.recommend.messaging.listener.RecommendTaskListener;
import com.navigator.knowledge.domain.recommend.messaging.producer.RecommendTaskProducer;
import com.navigator.knowledge.domain.summary.messaging.listener.SummaryTaskListener;
import com.navigator.knowledge.domain.summary.messaging.producer.SummaryTaskProducer;
import com.navigator.knowledge.domain.task.entity.Task;
import com.navigator.knowledge.domain.task.entity.TaskStatus;
import com.navigator.knowledge.domain.task.entity.TaskType;
import com.navigator.knowledge.domain.task.repository.TaskRepository;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIntegrationTest {

    private Long userId1;
    private Long userId2;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

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

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        User firstUser = userRepository.save(User.builder()
                .email("task-user-1@example.com")
                .name("Task User 1")
                .profileImageUrl("https://example.com/profile-1.png")
                .role(Role.USER)
                .careerGoal("Backend Developer")
                .build());

        User secondUser = userRepository.save(User.builder()
                .email("task-user-2@example.com")
                .name("Task User 2")
                .profileImageUrl("https://example.com/profile-2.png")
                .role(Role.USER)
                .careerGoal("AI Engineer")
                .build());

        userId1 = firstUser.getId();
        userId2 = secondUser.getId();
    }

    @Test
    @DisplayName("GET /api/tasks/subscribe/{taskId}는 다른 사용자의 작업 구독 시 403을 반환한다")
    void subscribe_returns403WhenTaskBelongsToAnotherUser() throws Exception {
        Task task = taskRepository.save(Task.builder()
                .taskId("task-subscribe-001")
                .userId(userId1)
                .taskType(TaskType.SUMMARY)
                .status(TaskStatus.PROCESSING)
                .expiresAt(LocalDateTime.now().plusSeconds(45))
                .build());

        mockMvc.perform(get("/api/tasks/subscribe/{taskId}", task.getTaskId())
                        .header("Authorization", authorizationHeader(userId2)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("TASK_ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value(
                        "해당 작업에 접근할 수 없습니다. taskId=task-subscribe-001, userId=" + userId2
                ))
                .andExpect(jsonPath("$.path").value("/api/tasks/subscribe/task-subscribe-001"));
    }

    private String authorizationHeader(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId, "ROLE_USER");
    }
}
