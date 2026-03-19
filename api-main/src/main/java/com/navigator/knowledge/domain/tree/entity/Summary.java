package com.navigator.knowledge.domain.tree.entity;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Summary")
public class Summary {
    @Id
    private Long summaryId;

    public Summary(Long summaryId) {
        this.summaryId = summaryId;
    }
}
