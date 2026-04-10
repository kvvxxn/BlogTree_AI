package com.navigator.knowledge.domain.recommend.dto;

import com.navigator.knowledge.domain.recommend.entity.Recommendation;

import java.time.LocalDateTime;

public record RecommendationResponseDto(
    Long recommendationId,
    String taskId,
    Long userId,
    String reason,
    String category,
    String topic,
    String keyword,
    LocalDateTime createdAt
) {
    public static RecommendationResponseDto from(Recommendation recommendation) {
        return new RecommendationResponseDto(
            recommendation.getRecommendationId(),
            recommendation.getTask().getTaskId(),
            recommendation.getUserId(),
            recommendation.getReason(),
            recommendation.getCategory(),
            recommendation.getTopic(),
            recommendation.getKeyword(),
            recommendation.getCreatedAt()
        );
    }
}
