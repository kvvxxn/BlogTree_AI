package com.navigator.knowledge.domain.tree.service;

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
    private final KnowledgeRepository knowledgeRepository;

    @Transactional
    public void saveKnowledgePath(Long userId, String category, String Topic, String Keyword, Long summaryId, List<Double> embedding) {
        knowledgeRepository.addKnowledgeWithSummary(userId, category, Topic, Keyword, summaryId, embedding);
    }

    @Transactional
    public void addSummaryToSimilarKeyword(Long userId, Long summaryId, List<Double> embedding) {
        Long keywordId = knowledgeRepository.findMostSimilarKeywordId(userId, embedding)
            .orElseThrow(() -> new IllegalArgumentException("No such keyword"));

        knowledgeRepository.createAndAttachSummaryToKeyword(userId, keywordId, summaryId, embedding);
    }

    @Transactional(readOnly = true)
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
