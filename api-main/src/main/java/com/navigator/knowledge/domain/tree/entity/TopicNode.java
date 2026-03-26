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

@Node("Topic")
@Getter
@Setter
@NoArgsConstructor
public class Topic {
    @Id
    @GeneratedValue
    private Long id;
    
    private String name;

    @Relationship(type = "HAS_KEYWORD", direction = Relationship.Direction.OUTGOING)
    private Set<Keyword> keywords = new HashSet<>();

    public Topic(String name) { this.name = name; }
    public void addKeyword(Keyword keyword) { this.keywords.add(keyword); }
}