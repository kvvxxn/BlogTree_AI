package com.navigator.knowledge.domain.summary.dto;

import com.navigator.knowledge.domain.summary.entity.Summary;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SummaryResponseDto(
        String sourceUrl,
        String content,
        LocalDateTime createdAt
) {
    public static SummaryResponseDto from(Summary summary) {
        return SummaryResponseDto.builder()
                .sourceUrl(summary.getSourceUrl())
                .content(summary.getContent())
                .createdAt(summary.getCreatedAt())
                .build();
    }
}