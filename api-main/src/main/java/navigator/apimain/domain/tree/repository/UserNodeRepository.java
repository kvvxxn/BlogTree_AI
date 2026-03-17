package navigator.apimain.domain.tree.repository;

import navigator.apimain.domain.tree.dto.KnowledgePathDto;
import navigator.apimain.domain.tree.entity.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;

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
    @Query("MATCH (u:UserNode {userId: $userId}) " +
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
    @Query("MATCH (u:UserNode {userId: $userId})-[:OWNS_CATEGORY]->(c:Category) " +
        "OPTIONAL MATCH (c)-[:HAS_TOPIC]->(t:Topic) " +
        "OPTIONAL MATCH (t)-[:HAS_KEYWORD]->(k:Keyword) " +
        "RETURN c.name AS categoryName, t.name AS topicName, k.name AS keywordName")
    List<KnowledgePathDto> findAllKnowledgeByUserId(Long userId);
}
