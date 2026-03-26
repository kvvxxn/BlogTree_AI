package com.navigator.knowledge.domain.tree.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.List;

@Node("Summary")
@Getter
@NoArgsConstructor
public class SummaryNode {
    @Id
    private Long summaryId;

    private List<Double> embedding;

    public SummaryNode(Long summaryId, List<Double> embedding) {
        this.summaryId = summaryId;
        this.embedding = embedding;
    }
}