package navigator.apimain.domain.tree.entity;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Node("Category")
public class Category {
    @Id @GeneratedValue
    private Long id;
    private String name;

    @Relationship(type = "HAS_TOPIC", direction = Relationship.Direction.OUTGOING)
    private Set<Topic> topics = new HashSet<>();

    public Category(String name) { this.name = name; }
    public void addTopic(Topic topic) { this.topics.add(topic); }
}