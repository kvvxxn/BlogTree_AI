package com.navigator.knowledge.domain.stats.dto;

import java.time.LocalDateTime;

public record RecentKeywordDto(
        String keyword,
        LocalDateTime readAt
) {
}
