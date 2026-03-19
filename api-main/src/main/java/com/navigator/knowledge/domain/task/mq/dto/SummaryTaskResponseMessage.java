package com.navigator.knowledge.domain.task.mq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SummaryTaskResponseDto(
    @JsonProperty("task_id") String taskId,
    @JsonProperty("user_id") String userId,
    @JsonProperty("status") String status, // SUCCESS, PARTIAL_SUCCESS, FAILED

    // SUCCESS, PARTIAL_SUCCESS일 때 값이 들어옴 (FAILED일 땐 null)
    @JsonProperty("data") ResultData data,

    // FAILED일 때 값이 들어옴 (성공일 땐 null)
    @JsonProperty("error") ErrorData error
) {
    // 1. data 객체 내부
    public record ResultData(
        @JsonProperty("summary_content") String summaryContent,
        @JsonProperty("knowledge_tree") KnowledgeTree knowledgeTree
    ) {}

    // 2. knowledge_tree 객체 내부
    public record KnowledgeTree(
        @JsonProperty("category") String category,
        @JsonProperty("topic") String topic,
        @JsonProperty("keyword") String keyword
    ) {}

    // 3. error 객체 내부
    public record ErrorData(
        @JsonProperty("code") String code,
        @JsonProperty("message") String message
    ) {}
}