package com.navigator.knowledge.domain.stats.dto;

public record TopicReadCountDto(
        String topic,
        long count
) {
}
