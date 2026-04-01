package com.navigator.knowledge.domain.tree.service;

import com.navigator.knowledge.domain.tree.exception.SimilarKeywordNotFoundException;
import lombok.RequiredArgsConstructor;
import com.navigator.knowledge.domain.tree.dto.KnowledgePathDto;
import com.navigator.knowledge.domain.tree.repository.KnowledgeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private static final String NEO4J_TRANSACTION_MANAGER = "neo4jTransactionManager";

    private final KnowledgeRepository knowledgeRepository;

    @Transactional(transactionManager = NEO4J_TRANSACTION_MANAGER)
    public void saveKnowledgePath(Long userId, String category, String topic, String keyword, Long summaryId, List<Double> embedding) {
        knowledgeRepository.addKnowledgeWithSummary(userId, category, topic, keyword, summaryId, embedding);
    }

    @Transactional(transactionManager = NEO4J_TRANSACTION_MANAGER)
    public void addSummaryToSimilarKeyword(Long userId, Long summaryId, List<Double> embedding) {
        Long keywordId = knowledgeRepository.findMostSimilarKeywordId(userId, embedding)
            .orElseThrow(() -> new SimilarKeywordNotFoundException(userId));

        knowledgeRepository.createAndAttachSummaryToKeyword(userId, keywordId, summaryId, embedding);
    }

    @Transactional(transactionManager = NEO4J_TRANSACTION_MANAGER, readOnly = true)
    public Map<String, Map<String, List<String>>> getKnowledgeTree(Long userId) {
        List<KnowledgePathDto> paths = knowledgeRepository.findAllKnowledgeByUserId(userId);

        return paths.stream()
            .filter(path -> path.categoryName() != null)
            .collect(Collectors.groupingBy(
                KnowledgePathDto::categoryName,
                Collectors.groupingBy(
                    path -> path.topicName() != null ? path.topicName() : "No Topic",
                    Collectors.mapping(
                        path -> path.keywordName() != null ? path.keywordName() : "No Keyword",
                        Collectors.toList()
                    )
                )
            ));
    }
}
