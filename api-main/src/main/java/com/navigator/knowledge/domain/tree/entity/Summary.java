package com.navigator.knowledge.domain.tree.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Summary")
@Getter
@Setter
@NoArgsConstructor
public class Summary {
    @Id
    private Long summaryId;

    // 선택 사항: VectorDB에 저장할 때 사용했던 원본 텍스트나 핵심 메타데이터를
    // Neo4j에도 일부 저장해두면 그래프 탐색만으로도 뷰를 구성하기 편리합니다.
    // private String content;
    // private String sourceUrl;

    public Summary(Long summaryId) {
        this.summaryId = summaryId;
    }
}
