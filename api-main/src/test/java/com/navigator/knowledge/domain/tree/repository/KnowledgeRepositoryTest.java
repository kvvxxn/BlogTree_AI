package com.navigator.knowledge.domain.tree.repository;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataNeo4jTest
class KnowledgeRepositoryTest {

    private static final Neo4j neo4j = Neo4jBuilders.newInProcessBuilder()
        .withDisabledServer()
        .build();

    @DynamicPropertySource
    static void configureNeo4j(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", neo4j::boltURI);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> null);
    }

    @AfterAll
    static void tearDown() {
        neo4j.close();
    }

    @Autowired
    private KnowledgeRepository knowledgeRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    @BeforeEach
    void clearDatabase() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }

    @Test
    @DisplayName("입력 임베딩과 가장 유사한 Summary에 연결된 Keyword id를 반환한다")
    void findMostSimilarKeywordId_returnsKeywordConnectedToMostSimilarSummary() {
        Long userId = 1L;

        Long expectedKeywordId = neo4jClient.query("""
                CREATE (u:User {userId: $userId})
                CREATE (u)-[:OWNS_CATEGORY]->(:Category {name: 'Backend'})
                      -[:HAS_TOPIC]->(:Topic {name: 'Database'})
                      -[:HAS_KEYWORD]->(best:Keyword {name: 'PostgreSQL'})
                      -[:DESCRIBED_BY]->(:Summary {summaryId: 100, embedding: [1.0, 0.0, 0.0]})
                CREATE (u)-[:OWNS_CATEGORY]->(:Category {name: 'Backend2'})
                      -[:HAS_TOPIC]->(:Topic {name: 'Cache'})
                      -[:HAS_KEYWORD]->(:Keyword {name: 'Redis'})
                      -[:DESCRIBED_BY]->(:Summary {summaryId: 200, embedding: [0.0, 1.0, 0.0]})
                RETURN id(best) AS keywordId
                """)
            .bind(userId).to("userId")
            .fetchAs(Long.class)
            .one()
            .orElseThrow();

        Optional<Long> result = knowledgeRepository.findMostSimilarKeywordId(userId, List.of(0.9, 0.1, 0.0));

        assertThat(result).contains(expectedKeywordId);
    }

    @Test
    @DisplayName("찾은 Keyword 내부 id로 새 Summary를 연결한다")
    void createAndAttachSummaryToKeyword_attachesSummaryToMatchedKeyword() {
        Long userId = 1L;

        Long keywordId = neo4jClient.query("""
                CREATE (u:User {userId: $userId})
                CREATE (u)-[:OWNS_CATEGORY]->(:Category {name: 'Backend'})
                      -[:HAS_TOPIC]->(:Topic {name: 'Database'})
                      -[:HAS_KEYWORD]->(k:Keyword {name: 'MySQL'})
                RETURN id(k) AS keywordId
                """)
            .bind(userId).to("userId")
            .fetchAs(Long.class)
            .one()
            .orElseThrow();

        knowledgeRepository.createAndAttachSummaryToKeyword(userId, keywordId, 300L, List.of(0.1, 0.2, 0.3));

        Long attachedCount = neo4jClient.query("""
                MATCH (u:User {userId: $userId})-[:OWNS_CATEGORY]->(:Category)-[:HAS_TOPIC]->(:Topic)
                      -[:HAS_KEYWORD]->(k:Keyword)-[:DESCRIBED_BY]->(s:Summary {summaryId: $summaryId})
                WHERE id(k) = $keywordId
                RETURN count(s) AS attachedCount
                """)
            .bind(userId).to("userId")
            .bind(keywordId).to("keywordId")
            .bind(300L).to("summaryId")
            .fetchAs(Long.class)
            .one()
            .orElseThrow();

        assertThat(attachedCount).isEqualTo(1L);
    }
}
