package com.navigator.knowledge.domain.recommend.messaging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RecommendTaskResponseMessage(
    @JsonProperty("task_id") String taskId,
    @JsonProperty("user_id") Long userId,
    @JsonProperty("status") String status,
    @JsonProperty("data") ResultData data,
    @JsonProperty("error") ErrorData error
) {
    public record ResultData(
        @JsonProperty("reason") String reason,
        @JsonProperty("category") String category,
        @JsonProperty("topic") String topic,
        @JsonProperty("keyword") String keyword
    ) {}

    public record ErrorData(
        @JsonProperty("code") String code,
        @JsonProperty("message") String message
    ) {}
}
