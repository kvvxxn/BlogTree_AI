package com.navigator.knowledge.domain.tree.entity;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Node("Keyword")
public class Keyword {
    @Id
    @GeneratedValue
    private Long id;
    private String name;

    @Relationship(type = "CONTAINS_SUMMARY", direction = Relationship.Direction.OUTGOING)
    private Set<Summary> summaries = new HashSet<>();

    public Keyword(String name) { this.name = name; }
    public void addSummary(Summary summary) { this.summaries.add(summary); }
}