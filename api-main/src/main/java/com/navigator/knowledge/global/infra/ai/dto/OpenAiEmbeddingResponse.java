package com.navigator.knowledge.global.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record OpenAiEmbeddingResponse(
    String object,
    List<EmbeddingData> data,
    String model,
    Usage usage
) {
    public record EmbeddingData(
        String object,
        List<Double> embedding,
        int index
    ) {}

    public record Usage(
        @JsonProperty("prompt_tokens") int promptTokens,
        @JsonProperty("total_tokens") int totalTokens
    ) {}
}