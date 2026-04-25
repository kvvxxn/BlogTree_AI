package com.navigator.knowledge.domain.tree.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class SummaryVectorRepository {

    private final JdbcTemplate jdbcTemplate;

    public SummaryVectorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void updateEmbedding(Long summaryId, List<Double> embedding, String embeddingModel) {
        jdbcTemplate.update(
            "UPDATE summaries SET embedding = CAST(? AS vector), embedding_model = ? WHERE summary_id = ?",
            toVectorLiteral(embedding),
            embeddingModel,
            summaryId
        );
    }

    public Optional<Long> findNearestKeywordId(Long userId, List<Double> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            return Optional.empty();
        }

        List<Long> result = jdbcTemplate.query(
            """
                SELECT keyword_id
                FROM summaries
                WHERE user_id = ?
                  AND keyword_id IS NOT NULL
                  AND embedding IS NOT NULL
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT 1
                """,
            (rs, rowNum) -> rs.getLong("keyword_id"),
            userId,
            toVectorLiteral(embedding)
        );

        return result.stream().findFirst();
    }

    private String toVectorLiteral(List<Double> embedding) {
        return embedding.stream()
            .map(value -> BigDecimal.valueOf(value).stripTrailingZeros().toPlainString())
            .collect(Collectors.joining(",", "[", "]"));
    }
}
