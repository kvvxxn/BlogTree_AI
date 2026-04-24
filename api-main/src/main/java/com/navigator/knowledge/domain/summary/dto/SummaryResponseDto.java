package com.navigator.knowledge.domain.summary.dto;

import com.navigator.knowledge.domain.summary.entity.Summary;
import com.navigator.knowledge.domain.tree.entity.KnowledgeKeyword;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SummaryResponseDto(
        Long summaryId,
        String taskId,
        String category,
        String topic,
        String keyword,
        String sourceUrl,
        String content,
        LocalDateTime createdAt
) {
    public static SummaryResponseDto from(Summary summary) {
        KnowledgeKeyword knowledgeKeyword = summary.getKeyword();
        String category = null;
        String topic = null;
        String keyword = null;

        if (knowledgeKeyword != null) {
            keyword = knowledgeKeyword.getName();
            topic = knowledgeKeyword.getTopic().getName();
            category = knowledgeKeyword.getTopic().getCategory().getName();
        }

        return SummaryResponseDto.builder()
                .summaryId(summary.getSummaryId())
                .taskId(summary.getTask().getTaskId())
                .category(category)
                .topic(topic)
                .keyword(keyword)
                .sourceUrl(summary.getSourceUrl())
                .content(summary.getContent())
                .createdAt(summary.getCreatedAt())
                .build();
    }
}
