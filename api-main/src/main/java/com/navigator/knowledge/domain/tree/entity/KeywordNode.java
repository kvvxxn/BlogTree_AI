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

@Node("Keyword")
@Getter
@NoArgsConstructor
public class KeywordNode {
    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @Relationship(type = "DESCRIBED_BY", direction = Relationship.Direction.OUTGOING)
    private Set<SummaryNode> summaries = new HashSet<>();

    public KeywordNode(String name) {
        this.name = name;
    }

    public void addSummary(SummaryNode summaryNode) { this.summaries.add(summaryNode); }
}