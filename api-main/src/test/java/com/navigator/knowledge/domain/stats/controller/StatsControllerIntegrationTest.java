package com.navigator.knowledge.domain.stats.controller;

import com.navigator.knowledge.domain.recommend.messaging.listener.RecommendTaskListener;
import com.navigator.knowledge.domain.recommend.messaging.producer.RecommendTaskProducer;
import com.navigator.knowledge.domain.summary.entity.Summary;
import com.navigator.knowledge.domain.summary.messaging.listener.SummaryTaskListener;
import com.navigator.knowledge.domain.summary.messaging.producer.SummaryTaskProducer;
import com.navigator.knowledge.domain.summary.repository.SummaryRepository;
import com.navigator.knowledge.domain.task.entity.Task;
import com.navigator.knowledge.domain.task.entity.TaskStatus;
import com.navigator.knowledge.domain.task.entity.TaskType;
import com.navigator.knowledge.domain.task.repository.TaskRepository;
import com.navigator.knowledge.domain.task.sse.SseEmitterService;
import com.navigator.knowledge.domain.tree.entity.KnowledgeCategory;
import com.navigator.knowledge.domain.tree.entity.KnowledgeKeyword;
import com.navigator.knowledge.domain.tree.entity.KnowledgeTopic;
import com.navigator.knowledge.domain.tree.repository.KnowledgeCategoryRepository;
import com.navigator.knowledge.domain.tree.repository.KnowledgeKeywordRepository;
import com.navigator.knowledge.domain.tree.repository.KnowledgeTopicRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class StatsControllerIntegrationTest {

    private Long userId1;
    private Long userId2;
    private KnowledgeKeyword jpaKeyword;
    private KnowledgeKeyword redisKeyword;
    private KnowledgeKeyword kafkaKeyword;
    private KnowledgeKeyword mysqlKeyword;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private SummaryRepository summaryRepository;

    @Autowired
    private KnowledgeCategoryRepository knowledgeCategoryRepository;

    @Autowired
    private KnowledgeTopicRepository knowledgeTopicRepository;

    @Autowired
    private KnowledgeKeywordRepository knowledgeKeywordRepository;

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
        summaryRepository.deleteAll();
        knowledgeKeywordRepository.deleteAll();
        knowledgeTopicRepository.deleteAll();
        knowledgeCategoryRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();

        User firstUser = userRepository.save(User.builder()
                .email("stats-user-1@example.com")
                .name("Stats User 1")
                .profileImageUrl("https://example.com/profile-1.png")
                .role(Role.USER)
                .careerGoal("Backend Developer")
                .build());
        User secondUser = userRepository.save(User.builder()
                .email("stats-user-2@example.com")
                .name("Stats User 2")
                .profileImageUrl("https://example.com/profile-2.png")
                .role(Role.USER)
                .careerGoal("AI Engineer")
                .build());

        userId1 = firstUser.getId();
        userId2 = secondUser.getId();

        KnowledgeCategory backendCategory = knowledgeCategoryRepository.save(KnowledgeCategory.builder()
                .userId(userId1)
                .name("Backend")
                .build());
        KnowledgeTopic springTopic = knowledgeTopicRepository.save(KnowledgeTopic.builder()
                .category(backendCategory)
                .name("Spring")
                .build());
        KnowledgeTopic infraTopic = knowledgeTopicRepository.save(KnowledgeTopic.builder()
                .category(backendCategory)
                .name("Infra")
                .build());
        jpaKeyword = knowledgeKeywordRepository.save(KnowledgeKeyword.builder()
                .topic(springTopic)
                .name("JPA")
                .build());
        redisKeyword = knowledgeKeywordRepository.save(KnowledgeKeyword.builder()
                .topic(infraTopic)
                .name("Redis")
                .build());
        kafkaKeyword = knowledgeKeywordRepository.save(KnowledgeKeyword.builder()
                .topic(infraTopic)
                .name("Kafka")
                .build());

        KnowledgeCategory databaseCategory = knowledgeCategoryRepository.save(KnowledgeCategory.builder()
                .userId(userId1)
                .name("Database")
                .build());
        KnowledgeTopic mysqlTopic = knowledgeTopicRepository.save(KnowledgeTopic.builder()
                .category(databaseCategory)
                .name("MySQL")
                .build());
        mysqlKeyword = knowledgeKeywordRepository.save(KnowledgeKeyword.builder()
                .topic(mysqlTopic)
                .name("Index")
                .build());
    }

    @Test
    @DisplayName("GET /api/stats는 사용자 통계와 카테고리 비율을 반환한다")
    void getStats_returnsUserStats() throws Exception {
        saveSummary("stats-task-001", userId1, jpaKeyword);
        saveSummary("stats-task-002", userId1, jpaKeyword);
        saveSummary("stats-task-003", userId1, redisKeyword);
        saveSummary("stats-task-004", userId1, kafkaKeyword);
        saveSummary("stats-task-005", userId1, mysqlKeyword);
        saveSummary("stats-task-006", userId1, null);
        saveOtherUserSummary();

        mockMvc.perform(get("/api/stats")
                        .header("Authorization", authorizationHeader(userId1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careerGoal").value("Backend Developer"))
                .andExpect(jsonPath("$.totalReadCount").value(6))
                .andExpect(jsonPath("$.categoryStats", hasSize(2)))
                .andExpect(jsonPath("$.categoryStats[0].category").value("Backend"))
                .andExpect(jsonPath("$.categoryStats[0].count").value(4))
                .andExpect(jsonPath("$.categoryStats[0].percentage").value(80.0))
                .andExpect(jsonPath("$.categoryStats[1].category").value("Database"))
                .andExpect(jsonPath("$.categoryStats[1].count").value(1))
                .andExpect(jsonPath("$.categoryStats[1].percentage").value(20.0))
                .andExpect(jsonPath("$.topTopics", hasSize(3)))
                .andExpect(jsonPath("$.topTopics[0].topic").value("Infra"))
                .andExpect(jsonPath("$.topTopics[0].count").value(2))
                .andExpect(jsonPath("$.topTopics[1].topic").value("Spring"))
                .andExpect(jsonPath("$.topTopics[1].count").value(2))
                .andExpect(jsonPath("$.topTopics[2].topic").value("MySQL"))
                .andExpect(jsonPath("$.topTopics[2].count").value(1))
                .andExpect(jsonPath("$.recentKeywords", hasSize(5)))
                .andExpect(jsonPath("$.recentKeywords[0].keyword").value("Index"))
                .andExpect(jsonPath("$.recentKeywords[1].keyword").value("Kafka"))
                .andExpect(jsonPath("$.recentKeywords[2].keyword").value("Redis"))
                .andExpect(jsonPath("$.recentKeywords[3].keyword").value("JPA"))
                .andExpect(jsonPath("$.recentKeywords[4].keyword").value("JPA"))
                .andExpect(jsonPath("$.recentKeywords[0].readAt").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/stats는 읽은 블로그가 없으면 빈 통계 목록을 반환한다")
    void getStats_returnsEmptyStatsWhenNoSummaries() throws Exception {
        mockMvc.perform(get("/api/stats")
                        .header("Authorization", authorizationHeader(userId1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careerGoal").value("Backend Developer"))
                .andExpect(jsonPath("$.totalReadCount").value(0))
                .andExpect(jsonPath("$.categoryStats", hasSize(0)))
                .andExpect(jsonPath("$.topTopics", hasSize(0)))
                .andExpect(jsonPath("$.recentKeywords", hasSize(0)));
    }

    private void saveSummary(String taskId, Long userId, KnowledgeKeyword keyword) {
        Task task = taskRepository.save(Task.builder()
                .taskId(taskId)
                .userId(userId)
                .sourceUrl("https://example.com/" + taskId)
                .taskType(TaskType.SUMMARY)
                .status(TaskStatus.SUCCESS)
                .expiresAt(LocalDateTime.now().plusSeconds(45))
                .build());
        Summary summary = Summary.builder()
                .task(task)
                .userId(userId)
                .sourceUrl(task.getSourceUrl())
                .content("요약 본문")
                .build();
        if (keyword != null) {
            summary.assignKeyword(keyword);
        }
        summaryRepository.saveAndFlush(summary);
    }

    private void saveOtherUserSummary() {
        KnowledgeCategory category = knowledgeCategoryRepository.save(KnowledgeCategory.builder()
                .userId(userId2)
                .name("Frontend")
                .build());
        KnowledgeTopic topic = knowledgeTopicRepository.save(KnowledgeTopic.builder()
                .category(category)
                .name("React")
                .build());
        KnowledgeKeyword keyword = knowledgeKeywordRepository.save(KnowledgeKeyword.builder()
                .topic(topic)
                .name("Hooks")
                .build());
        saveSummary("stats-task-other", userId2, keyword);
    }

    private String authorizationHeader(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId, "ROLE_USER");
    }
}
