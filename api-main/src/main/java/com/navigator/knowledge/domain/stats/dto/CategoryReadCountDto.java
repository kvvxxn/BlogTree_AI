package com.navigator.knowledge.domain.stats.dto;

public record CategoryReadCountDto(
        String category,
        long count
) {
}
