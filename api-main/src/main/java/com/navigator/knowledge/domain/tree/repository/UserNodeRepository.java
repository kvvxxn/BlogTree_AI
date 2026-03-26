package com.navigator.knowledge.domain.tree.repository;

import com.navigator.knowledge.domain.tree.dto.KnowledgePathDto;
import com.navigator.knowledge.domain.tree.entity.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserNodeRepository extends Neo4jRepository<UserNode, Long> {

    /**
     * Creates or updates a UserNode with the given userId.
     *
     * @param userId The unique identifier of the user.
     */
    @Query("MERGE (u:User {userId: $userId}) RETURN u")
    UserNode saveUserNode(Long userId);

    /**
     * Adds a hierarchical knowledge path (Category -> Topic -> Keyword) to a specific user.
     * Uses MERGE to ensure nodes and relationships are created only if they do not already exist.
     *
     * @param userId       The unique identifier of the user.
     * @param categoryName The name of the category.
     * @param topicName    The name of the topic.
     * @param keywordName  The name of the keyword.
     */
    @Query("MATCH (u:User {userId: $userId}) " +
        "MERGE (u)-[:OWNS_CATEGORY]->(c:Category {name: $categoryName}) " +
        "MERGE (c)-[:HAS_TOPIC]->(t:Topic {name: $topicName}) " +
        "MERGE (t)-[:HAS_KEYWORD]->(k:Keyword {name: $keywordName}) " +
        "RETURN u")
    void addKnowledge(Long userId, String categoryName, String topicName, String keywordName);

    /**
     * Retrieves all knowledge paths associated with a specific user.
     *
     * @param userId The unique identifier of the user.
     * @return A list of KnowledgePathDto containing category, topic, and keyword names.
     */
    @Query("MATCH (u:User {userId: $userId})-[:OWNS_CATEGORY]->(c:Category) " +
        "OPTIONAL MATCH (c)-[:HAS_TOPIC]->(t:Topic) " +
        "OPTIONAL MATCH (t)-[:HAS_KEYWORD]->(k:Keyword) " +
        "RETURN c.name AS categoryName, t.name AS topicName, k.name AS keywordName")
    List<KnowledgePathDto> findAllKnowledgeByUserId(Long userId);

    /**
     * Adds a hierarchical knowledge path and attaches a Summary node with its embedding.
     */
    @Query("MERGE (u:User {userId: $userId}) " +
        "MERGE (u)-[:OWNS_CATEGORY]->(c:Category {name: $categoryName}) " +
        "MERGE (c)-[:HAS_TOPIC]->(t:Topic {name: $topicName}) " +
        "MERGE (t)-[:HAS_KEYWORD]->(k:Keyword {name: $keywordName}) " +
        "CREATE (s:Summary {summaryId: $summaryId, embedding: $embedding}) " +
        "MERGE (k)-[:DESCRIBED_BY]->(s) " +
        "RETURN u")
    void addKnowledgeWithSummary(
        @Param("userId") Long userId,
        @Param("categoryName") String categoryName,
        @Param("topicName") String topicName,
        @Param("keywordName") String keywordName,
        @Param("summaryId") Long summaryId,
        @Param("embedding") List<Double> embedding
    );

    /**
     * 벡터 유사도를 기반으로 가장 유사한 Keyword 노드의 ID를 찾고, 해당 키워드에 새 Summary 노드를 연결합니다.
     * 
     * 주의: 이 쿼리를 사용하려면 Neo4j 데이터베이스에 벡터 인덱스가 미리 생성되어 있어야 합니다.
     * 인덱스가 없다면 코사인 유사도 연산 함수 (gds.similarity.cosine 또는 자체 계산 로직)를 이용한 전체 스캔 방식을 사용해야 합니다.
     * 아래는 벡터 유사도 연산을 수동으로 수행하여 가장 가까운 Keyword를 찾는 Cypher입니다. (O(N) 탐색)
     */
    @Query("MATCH (u:User {userId: $userId})-[:OWNS_CATEGORY]->(c:Category)-[:HAS_TOPIC]->(t:Topic)-[:HAS_KEYWORD]->(k:Keyword)-[:DESCRIBED_BY]->(existing_s:Summary) " +
           "WHERE existing_s.embedding IS NOT NULL " +
           "WITH k, existing_s, " +
           "     gds.similarity.cosine($embedding, existing_s.embedding) AS similarity " +
           "ORDER BY similarity DESC " +
           "LIMIT 1 " +
           "CREATE (new_s:Summary {summaryId: $summaryId, embedding: $embedding}) " +
           "MERGE (k)-[:DESCRIBED_BY]->(new_s) " +
           "RETURN k.name AS keywordName")
    Optional<String> attachSummaryToMostSimilarKeyword(
        @Param("userId") Long userId,
        @Param("summaryId") Long summaryId,
        @Param("embedding") List<Double> embedding
    );
}