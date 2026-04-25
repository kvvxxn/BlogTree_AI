package com.navigator.knowledge.domain.tree.service;

import com.navigator.knowledge.domain.summary.entity.Summary;
import com.navigator.knowledge.domain.summary.exception.SummaryNotFoundException;
import com.navigator.knowledge.domain.summary.repository.SummaryRepository;
import com.navigator.knowledge.domain.tree.exception.SimilarKeywordNotFoundException;
import com.navigator.knowledge.domain.tree.dto.KnowledgePathDto;
import com.navigator.knowledge.domain.tree.entity.KnowledgeCategory;
import com.navigator.knowledge.domain.tree.entity.KnowledgeKeyword;
import com.navigator.knowledge.domain.tree.entity.KnowledgeTopic;
import com.navigator.knowledge.domain.tree.repository.KnowledgeCategoryRepository;
import com.navigator.knowledge.domain.tree.repository.KnowledgeKeywordRepository;
import com.navigator.knowledge.domain.tree.repository.KnowledgeTopicRepository;
import com.navigator.knowledge.domain.tree.repository.SummaryVectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeCategoryRepository knowledgeCategoryRepository;
    private final KnowledgeTopicRepository knowledgeTopicRepository;
    private final KnowledgeKeywordRepository knowledgeKeywordRepository;
    private final SummaryRepository summaryRepository;
    private final SummaryVectorRepository summaryVectorRepository;

    @Value("${spring.ai.openai.embedding.options.model}")
    private String embeddingModel;

    @Transactional
    public void saveKnowledgePath(Long userId, String category, String topic, String keyword, Long summaryId, List<Double> embedding) {
        KnowledgeKeyword knowledgeKeyword = getOrCreateKeyword(userId, category, topic, keyword);
        Summary summary = summaryRepository.findById(summaryId)
            .orElseThrow(() -> new SummaryNotFoundException(summaryId));

        summary.assignKeyword(knowledgeKeyword);
        summaryRepository.flush();
        summaryVectorRepository.updateEmbedding(summaryId, embedding, embeddingModel);
    }

    @Transactional
    public void addSummaryToSimilarKeyword(Long userId, Long summaryId, List<Double> embedding) {
        Long keywordId = summaryVectorRepository.findNearestKeywordId(userId, embedding)
            .orElseThrow(() -> new SimilarKeywordNotFoundException(userId));

        KnowledgeKeyword keyword = knowledgeKeywordRepository.findById(keywordId)
            .orElseThrow(() -> new SimilarKeywordNotFoundException(userId));
        Summary summary = summaryRepository.findById(summaryId)
            .orElseThrow(() -> new SummaryNotFoundException(summaryId));

        summary.assignKeyword(keyword);
        summaryRepository.flush();
        summaryVectorRepository.updateEmbedding(summaryId, embedding, embeddingModel);
    }

    @Transactional(readOnly = true)
    public Map<String, Map<String, List<String>>> getKnowledgeTree(Long userId) {
        List<KnowledgePathDto> paths = knowledgeKeywordRepository.findAllKnowledgePathsByUserId(userId);

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

    private KnowledgeKeyword getOrCreateKeyword(Long userId, String categoryName, String topicName, String keywordName) {
        KnowledgeCategory category = knowledgeCategoryRepository.findByUserIdAndName(userId, categoryName)
            .orElseGet(() -> knowledgeCategoryRepository.save(
                KnowledgeCategory.builder()
                    .userId(userId)
                    .name(categoryName)
                    .build()
            ));

        KnowledgeTopic topic = knowledgeTopicRepository.findByCategoryAndName(category, topicName)
            .orElseGet(() -> knowledgeTopicRepository.save(
                KnowledgeTopic.builder()
                    .category(category)
                    .name(topicName)
                    .build()
            ));

        return knowledgeKeywordRepository.findByTopicAndName(topic, keywordName)
            .orElseGet(() -> knowledgeKeywordRepository.save(
                KnowledgeKeyword.builder()
                    .topic(topic)
                    .name(keywordName)
                    .build()
            ));
    }
}
