package com.navigator.knowledge.domain.recommend.messaging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record RecommendTaskRequestMessage(
    @JsonProperty("task_id") String taskId,
    @JsonProperty("user_id") Long userId,
    @JsonProperty("career_goal") String careerGoal,
    @JsonProperty("expired_at") String expiredAt,
    @JsonProperty("knowledge_tree") Map<String, Map<String, List<String>>> knowledgeTree
) {}
