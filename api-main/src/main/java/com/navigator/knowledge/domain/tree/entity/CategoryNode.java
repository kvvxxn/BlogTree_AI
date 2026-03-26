package com.navigator.knowledge.domain.tree.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Node("Category")
@Getter
@NoArgsConstructor
public class CategoryNode {
    @Id @GeneratedValue
    private Long id;
    
    private String name;

    @Relationship(type = "HAS_TOPIC", direction = Relationship.Direction.OUTGOING)
    private Set<TopicNode> topicNodes = new HashSet<>();

    public CategoryNode(String name) { this.name = name; }
    public void addTopic(TopicNode topicNode) { this.topicNodes.add(topicNode); }
}