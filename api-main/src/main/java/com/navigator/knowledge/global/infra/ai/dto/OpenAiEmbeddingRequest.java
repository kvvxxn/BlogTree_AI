package com.navigator.knowledge.global.infra.ai.dto;

public record OpenAiEmbeddingRequest(
    String input,
    String model
) {
}