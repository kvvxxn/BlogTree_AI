package com.navigator.knowledge.domain.recommend.service;

import com.navigator.knowledge.domain.recommend.dto.RecommendationResponseDto;
import com.navigator.knowledge.domain.recommend.entity.Recommendation;
import com.navigator.knowledge.domain.recommend.exception.RecommendationAccessDeniedException;
import com.navigator.knowledge.domain.recommend.exception.RecommendationNotFoundException;
import com.navigator.knowledge.domain.recommend.repository.RecommendationRepository;
import com.navigator.knowledge.domain.task.entity.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final String JPA_TRANSACTION_MANAGER = "jpaTransactionManager";

    private final RecommendationRepository recommendationRepository;

    @Transactional(transactionManager = JPA_TRANSACTION_MANAGER)
    public Recommendation saveRecommendation(
        Task task,
        Long userId,
        String reason,
        String category,
        String topic,
        String keyword
    ) {
        Recommendation recommendation = Recommendation.builder()
            .task(task)
            .userId(userId)
            .reason(reason)
            .category(category)
            .topic(topic)
            .keyword(keyword)
            .build();
        return recommendationRepository.save(recommendation);
    }

    @Transactional(transactionManager = JPA_TRANSACTION_MANAGER)
    public Recommendation findOrCreateRecommendation(
        Task task,
        Long userId,
        String reason,
        String category,
        String topic,
        String keyword
    ) {
        return recommendationRepository.findByTask_TaskId(task.getTaskId())
            .orElseGet(() -> saveRecommendation(task, userId, reason, category, topic, keyword));
    }

    @Transactional(transactionManager = JPA_TRANSACTION_MANAGER, readOnly = true)
    public RecommendationResponseDto getRecommendation(Long userId, Long recommendationId) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
            .orElseThrow(() -> new RecommendationNotFoundException(recommendationId));
        if (!Objects.equals(recommendation.getUserId(), userId)) {
            throw new RecommendationAccessDeniedException(recommendationId, userId);
        }
        return RecommendationResponseDto.from(recommendation);
    }
}
