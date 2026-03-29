package com.navigator.knowledge.domain.summary.messaging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record SummaryTaskRequestMessage(
    @JsonProperty("task_id") String taskId,
    @JsonProperty("user_id") Long userId,
    @JsonProperty("career_goal") String career_goal,
    @JsonProperty("source_url") String sourceUrl,
    @JsonProperty("expired_at") String expiredAt,

    // 계층 구조: Map<Category, Map<Topic, List<Keyword>>>
    @JsonProperty("knowledge_tree")
    Map<String, Map<String, List<String>>> knowledgeTree
) {}