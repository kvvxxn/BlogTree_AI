package com.navigator.knowledge.domain.tree.repository;

import com.navigator.knowledge.domain.tree.dto.KnowledgePathDto;
import com.navigator.knowledge.domain.tree.entity.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KnowledgeRepository extends Neo4jRepository<UserNode, Long> {

    /**
     * Creates or updates a UserNode with the given userId.
     *
     * @param userId The unique identifier of the user.
     */
    @Query("MERGE (u:User {userId: $userId}) RETURN u")
    UserNode saveUserNode(Long userId);

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

    @Query("MATCH (u:User {userId: $userId})-[:OWNS_CATEGORY]->(:Category)-[:HAS_TOPIC]->(:Topic)-[:HAS_KEYWORD]->(k:Keyword)-[:DESCRIBED_BY]->(existing_s:Summary) " +
        "WHERE existing_s.embedding IS NOT NULL " +
        "  AND size(existing_s.embedding) = size($embedding) " +
        "  AND size($embedding) > 0 " +
        "WITH k, existing_s, " +
        "     reduce(dot = 0.0, i IN range(0, size($embedding) - 1) | dot + ($embedding[i] * existing_s.embedding[i])) AS dotProduct, " +
        "     sqrt(reduce(norm = 0.0, value IN $embedding | norm + (value * value))) AS inputNorm, " +
        "     sqrt(reduce(norm = 0.0, value IN existing_s.embedding | norm + (value * value))) AS existingNorm " +
        "WITH k, CASE " +
        "          WHEN inputNorm = 0.0 OR existingNorm = 0.0 THEN -1.0 " +
        "          ELSE dotProduct / (inputNorm * existingNorm) " +
        "        END AS similarity " +
        "ORDER BY similarity DESC " +
        "LIMIT 1 " +
        "RETURN id(k)")
    Optional<Long> findMostSimilarKeywordId(
        @Param("userId") Long userId,
        @Param("embedding") List<Double> embedding
    );

    @Query("MATCH (u:User {userId: $userId})-[:OWNS_CATEGORY]->(:Category)-[:HAS_TOPIC]->(:Topic)-[:HAS_KEYWORD]->(k:Keyword) " +
        "WHERE id(k) = $keywordId " +
        "CREATE (new_s:Summary {summaryId: $summaryId, embedding: $embedding}) " +
        "MERGE (k)-[:DESCRIBED_BY]->(new_s)")
    void createAndAttachSummaryToKeyword(
        @Param("userId") Long userId,
        @Param("keywordId") Long keywordId,
        @Param("summaryId") Long summaryId,
        @Param("embedding") List<Double> embedding
    );

}