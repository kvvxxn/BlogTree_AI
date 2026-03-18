package com.navigator.knowledge.domain.task.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record TaskRequestDto(
    @JsonProperty("task_id") String taskId,
    @JsonProperty("user_id") String userId,
    @JsonProperty("career_goal") String career_goal,
    @JsonProperty("source_url") String sourceUrl,
    @JsonProperty("expired_at") String expiration,

    // 계층 구조: Map<Category, Map<Topic, List<Keyword>>>
    Map<String, Map<String, List<String>>> context
) {}