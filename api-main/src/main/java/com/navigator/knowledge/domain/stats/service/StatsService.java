package com.navigator.knowledge.domain.stats.service;

import com.navigator.knowledge.domain.stats.dto.CategoryReadCountDto;
import com.navigator.knowledge.domain.stats.dto.RecentKeywordDto;
import com.navigator.knowledge.domain.stats.dto.StatsResponseDto;
import com.navigator.knowledge.domain.stats.dto.TopicReadCountDto;
import com.navigator.knowledge.domain.summary.repository.SummaryRepository;
import com.navigator.knowledge.domain.user.entity.User;
import com.navigator.knowledge.domain.user.exception.UserNotFoundException;
import com.navigator.knowledge.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private static final String JPA_TRANSACTION_MANAGER = "jpaTransactionManager";
    private static final int TOP_TOPIC_LIMIT = 3;
    private static final int RECENT_KEYWORD_LIMIT = 5;

    private final UserRepository userRepository;
    private final SummaryRepository summaryRepository;

    @Transactional(transactionManager = JPA_TRANSACTION_MANAGER, readOnly = true)
    public StatsResponseDto getStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        long totalReadCount = summaryRepository.countByUserId(userId);
        List<CategoryReadCountDto> categoryReadCounts = summaryRepository.findCategoryReadCounts(userId);
        long categorizedReadCount = categoryReadCounts.stream()
                .mapToLong(CategoryReadCountDto::count)
                .sum();
        List<StatsResponseDto.CategoryStatsDto> categoryStats = categoryReadCounts
                .stream()
                .map(category -> toCategoryStats(category, categorizedReadCount))
                .toList();
        List<StatsResponseDto.TopicStatsDto> topTopics = summaryRepository
                .findTopTopicReadCounts(userId, PageRequest.of(0, TOP_TOPIC_LIMIT))
                .stream()
                .map(this::toTopicStats)
                .toList();
        List<StatsResponseDto.RecentKeywordStatsDto> recentKeywords = summaryRepository
                .findRecentKeywords(userId, PageRequest.of(0, RECENT_KEYWORD_LIMIT))
                .stream()
                .map(this::toRecentKeywordStats)
                .toList();

        return new StatsResponseDto(
                user.getCareerGoal(),
                totalReadCount,
                categoryStats,
                topTopics,
                recentKeywords
        );
    }

    private StatsResponseDto.CategoryStatsDto toCategoryStats(CategoryReadCountDto category, long categorizedReadCount) {
        double percentage = categorizedReadCount == 0
                ? 0.0
                : Math.round((category.count() * 1000.0) / categorizedReadCount) / 10.0;
        return new StatsResponseDto.CategoryStatsDto(category.category(), category.count(), percentage);
    }

    private StatsResponseDto.TopicStatsDto toTopicStats(TopicReadCountDto topic) {
        return new StatsResponseDto.TopicStatsDto(topic.topic(), topic.count());
    }

    private StatsResponseDto.RecentKeywordStatsDto toRecentKeywordStats(RecentKeywordDto keyword) {
        return new StatsResponseDto.RecentKeywordStatsDto(keyword.keyword(), keyword.readAt());
    }
}
