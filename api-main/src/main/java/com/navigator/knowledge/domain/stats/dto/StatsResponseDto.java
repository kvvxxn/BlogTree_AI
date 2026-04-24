package com.navigator.knowledge.domain.stats.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StatsResponseDto(
        String careerGoal,
        long totalReadCount,
        List<CategoryStatsDto> categoryStats,
        List<TopicStatsDto> topTopics,
        List<RecentKeywordStatsDto> recentKeywords
) {

    public record CategoryStatsDto(
            String category,
            long count,
            double percentage
    ) {
    }

    public record TopicStatsDto(
            String topic,
            long count
    ) {
    }

    public record RecentKeywordStatsDto(
            String keyword,
            LocalDateTime readAt
    ) {
    }
}
