package com.navigator.knowledge.global.infra.ai;

import com.navigator.knowledge.global.infra.ai.dto.OpenAiEmbeddingRequest;
import com.navigator.knowledge.global.infra.ai.dto.OpenAiEmbeddingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class TextEmbeddingService {

    private final RestClient restClient;
    private final String embeddingModel;

    public TextEmbeddingService(
            @Qualifier("openAiRestClient") RestClient restClient,
            @Value("${spring.ai.openai.embedding.options.model}") String embeddingModel
    ) {
        this.restClient = restClient;
        this.embeddingModel = embeddingModel;
    }

    /**
     * 주어진 텍스트를 임베딩 벡터로 변환합니다.
     *
     * @param text 임베딩할 원본 텍스트
     * @return 텍스트의 임베딩 벡터 (List<Double>)
     */
    public List<Double> embedText(String text) {
        log.debug("Requesting embedding for text using RestClient...");

        var requestDto = new OpenAiEmbeddingRequest(text, embeddingModel);

        OpenAiEmbeddingResponse response = restClient.post()
                .uri("/v1/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestDto)
                .retrieve()
                .body(OpenAiEmbeddingResponse.class);

        return Optional.ofNullable(response)
                .map(OpenAiEmbeddingResponse::data)
                .filter(data -> !data.isEmpty())
                .map(data -> data.get(0).embedding())
                .orElseThrow(() -> new IllegalStateException("Invalid response from OpenAI embedding API."));
    }
}