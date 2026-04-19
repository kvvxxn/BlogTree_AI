package com.navigator.knowledge.domain.tree.service;

import com.navigator.knowledge.domain.summary.entity.Summary;
import com.navigator.knowledge.domain.summary.repository.SummaryRepository;
import com.navigator.knowledge.domain.task.entity.Task;
import com.navigator.knowledge.domain.task.entity.TaskStatus;
import com.navigator.knowledge.domain.task.entity.TaskType;
import com.navigator.knowledge.domain.task.repository.TaskRepository;
import com.navigator.knowledge.domain.user.entity.Role;
import com.navigator.knowledge.domain.user.entity.User;
import com.navigator.knowledge.domain.user.repository.UserRepository;
import com.navigator.knowledge.domain.tree.repository.SummaryVectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@Import({KnowledgeService.class, SummaryVectorRepository.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.ai.openai.embedding.options.model=test-model"
})
class KnowledgeServicePostgresIntegrationTest {

    private static final DockerImageName PGVECTOR_IMAGE =
        DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(PGVECTOR_IMAGE)
        .withDatabaseName("blogtree")
        .withUsername("postgres_user")
        .withPassword("postgres_password");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private SummaryRepository summaryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("""
            TRUNCATE TABLE recommendations, summaries, tasks, refresh_tokens,
            knowledge_keywords, knowledge_topics, knowledge_categories, users
            RESTART IDENTITY CASCADE
            """);
    }

    @Test
    @DisplayName("saveKnowledgePath는 계층 구조와 embedding을 Postgres에 저장하고 tree 조회 shape를 유지한다")
    void saveKnowledgePath_storesHierarchyEmbeddingAndTreeShape() {
        User user = saveUser("tree-user@example.com");
        Summary postgresSummary = saveSummary(user.getId(), "task-tree-1");
        Summary redisSummary = saveSummary(user.getId(), "task-tree-2");

        knowledgeService.saveKnowledgePath(
            user.getId(), "Backend", "Database", "PostgreSQL", postgresSummary.getSummaryId(), embeddingWithHotIndex(0)
        );
        knowledgeService.saveKnowledgePath(
            user.getId(), "Backend", "Database", "Redis", redisSummary.getSummaryId(), embeddingWithHotIndex(1)
        );

        Map<String, Map<String, List<String>>> tree = knowledgeService.getKnowledgeTree(user.getId());

        Summary storedPostgresSummary = summaryRepository.findById(postgresSummary.getSummaryId()).orElseThrow();
        String embeddingModel = jdbcTemplate.queryForObject(
            "SELECT embedding_model FROM summaries WHERE summary_id = ?",
            String.class,
            postgresSummary.getSummaryId()
        );
        Boolean hasEmbedding = jdbcTemplate.queryForObject(
            "SELECT embedding IS NOT NULL FROM summaries WHERE summary_id = ?",
            Boolean.class,
            postgresSummary.getSummaryId()
        );

        assertThat(storedPostgresSummary.getKeyword()).isNotNull();
        assertThat(storedPostgresSummary.getKeyword().getName()).isEqualTo("PostgreSQL");
        assertThat(embeddingModel).isEqualTo("test-model");
        assertThat(hasEmbedding).isTrue();
        assertThat(tree).containsKey("Backend");
        assertThat(tree.get("Backend")).containsKey("Database");
        assertThat(tree.get("Backend").get("Database")).containsExactly("PostgreSQL", "Redis");
    }

    @Test
    @DisplayName("addSummaryToSimilarKeyword는 pgvector cosine distance로 가장 가까운 keyword를 연결한다")
    void addSummaryToSimilarKeyword_assignsNearestKeywordUsingPgvector() {
        User user = saveUser("vector-user@example.com");
        Summary existingSummary = saveSummary(user.getId(), "task-vector-1");
        Summary newSummary = saveSummary(user.getId(), "task-vector-2");

        knowledgeService.saveKnowledgePath(
            user.getId(), "Backend", "Database", "PostgreSQL", existingSummary.getSummaryId(), embeddingWithHotIndex(0)
        );

        List<Double> nearbyEmbedding = embeddingWithPrimaryAndSecondary(0, 1, 0.95, 0.05);
        knowledgeService.addSummaryToSimilarKeyword(user.getId(), newSummary.getSummaryId(), nearbyEmbedding);

        Summary storedNewSummary = summaryRepository.findById(newSummary.getSummaryId()).orElseThrow();
        String embeddingModel = jdbcTemplate.queryForObject(
            "SELECT embedding_model FROM summaries WHERE summary_id = ?",
            String.class,
            newSummary.getSummaryId()
        );
        Boolean hasEmbedding = jdbcTemplate.queryForObject(
            "SELECT embedding IS NOT NULL FROM summaries WHERE summary_id = ?",
            Boolean.class,
            newSummary.getSummaryId()
        );

        assertThat(storedNewSummary.getKeyword()).isNotNull();
        assertThat(storedNewSummary.getKeyword().getName()).isEqualTo("PostgreSQL");
        assertThat(embeddingModel).isEqualTo("test-model");
        assertThat(hasEmbedding).isTrue();
    }

    private User saveUser(String email) {
        return userRepository.save(User.builder()
            .email(email)
            .name("Test User")
            .profileImageUrl("https://example.com/profile.png")
            .role(Role.USER)
            .careerGoal("Backend Developer")
            .build());
    }

    private Summary saveSummary(Long userId, String taskId) {
        Task task = taskRepository.save(Task.builder()
            .taskId(taskId)
            .userId(userId)
            .sourceUrl("https://example.com/" + taskId)
            .taskType(TaskType.SUMMARY)
            .status(TaskStatus.PROCESSING)
            .expiresAt(LocalDateTime.now().plusSeconds(45))
            .build());

        return summaryRepository.save(Summary.builder()
            .task(task)
            .userId(userId)
            .sourceUrl(task.getSourceUrl())
            .content("summary for " + taskId)
            .build());
    }

    private List<Double> embeddingWithHotIndex(int index) {
        return embeddingWithPrimaryAndSecondary(index, -1, 1.0, 0.0);
    }

    private List<Double> embeddingWithPrimaryAndSecondary(int primaryIndex, int secondaryIndex, double primaryValue, double secondaryValue) {
        List<Double> embedding = new ArrayList<>(1536);
        for (int i = 0; i < 1536; i++) {
            embedding.add(0.0);
        }
        embedding.set(primaryIndex, primaryValue);
        if (secondaryIndex >= 0) {
            embedding.set(secondaryIndex, secondaryValue);
        }
        return embedding;
    }
}
