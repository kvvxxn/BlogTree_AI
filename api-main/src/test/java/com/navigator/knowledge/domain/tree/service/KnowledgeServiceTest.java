package com.navigator.knowledge.domain.tree.service;

import com.navigator.knowledge.domain.summary.entity.Summary;
import com.navigator.knowledge.domain.summary.repository.SummaryRepository;
import com.navigator.knowledge.domain.tree.dto.KnowledgePathDto;
import com.navigator.knowledge.domain.tree.entity.KnowledgeCategory;
import com.navigator.knowledge.domain.tree.entity.KnowledgeKeyword;
import com.navigator.knowledge.domain.tree.entity.KnowledgeTopic;
import com.navigator.knowledge.domain.tree.exception.SimilarKeywordNotFoundException;
import com.navigator.knowledge.domain.tree.repository.KnowledgeCategoryRepository;
import com.navigator.knowledge.domain.tree.repository.KnowledgeKeywordRepository;
import com.navigator.knowledge.domain.tree.repository.KnowledgeTopicRepository;
import com.navigator.knowledge.domain.tree.repository.SummaryVectorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private KnowledgeCategoryRepository knowledgeCategoryRepository;

    @Mock
    private KnowledgeTopicRepository knowledgeTopicRepository;

    @Mock
    private KnowledgeKeywordRepository knowledgeKeywordRepository;

    @Mock
    private SummaryRepository summaryRepository;

    @Mock
    private SummaryVectorRepository summaryVectorRepository;

    @InjectMocks
    private KnowledgeService knowledgeService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(knowledgeService, "embeddingModel", "test-model");
    }

    @Test
    @DisplayName("지식 경로 저장 테스트")
    void saveKnowledgePathTest() {
        // Given
        Long userId = 1L;
        String category = "Backend";
        String topic = "Database";
        String keyword = "MySQL";
        Long summaryId = 100L;
        List<Double> embedding = Arrays.asList(0.1, 0.2, 0.3);
        KnowledgeCategory savedCategory = KnowledgeCategory.builder().userId(userId).name(category).build();
        KnowledgeTopic savedTopic = KnowledgeTopic.builder().category(savedCategory).name(topic).build();
        KnowledgeKeyword savedKeyword = KnowledgeKeyword.builder().topic(savedTopic).name(keyword).build();
        Summary summary = Summary.builder().userId(userId).sourceUrl("https://example.com").content("content").build();

        when(knowledgeCategoryRepository.findByUserIdAndName(userId, category)).thenReturn(Optional.empty());
        when(knowledgeCategoryRepository.save(any(KnowledgeCategory.class))).thenReturn(savedCategory);
        when(knowledgeTopicRepository.findByCategoryAndName(savedCategory, topic)).thenReturn(Optional.empty());
        when(knowledgeTopicRepository.save(any(KnowledgeTopic.class))).thenReturn(savedTopic);
        when(knowledgeKeywordRepository.findByTopicAndName(savedTopic, keyword)).thenReturn(Optional.empty());
        when(knowledgeKeywordRepository.save(any(KnowledgeKeyword.class))).thenReturn(savedKeyword);
        when(summaryRepository.findById(summaryId)).thenReturn(Optional.of(summary));

        // When
        knowledgeService.saveKnowledgePath(userId, category, topic, keyword, summaryId, embedding);

        // Then
        assertThat(summary.getKeyword()).isEqualTo(savedKeyword);
        verify(summaryVectorRepository).updateEmbedding(summaryId, embedding, "test-model");
    }

    @Test
    @DisplayName("유사한 키워드에 요약 추가 테스트 - 키워드가 존재하는 경우")
    void addSummaryToSimilarKeywordTest_KeywordExists() {
        // Given
        Long userId = 1L;
        Long summaryId = 100L;
        List<Double> embedding = Arrays.asList(0.1, 0.2, 0.3);
        Long expectedKeywordId = 10L;
        KnowledgeKeyword savedKeyword = KnowledgeKeyword.builder()
            .topic(KnowledgeTopic.builder()
                .category(KnowledgeCategory.builder().userId(userId).name("Backend").build())
                .name("Database")
                .build())
            .name("MySQL")
            .build();
        Summary summary = Summary.builder().userId(userId).sourceUrl("https://example.com").content("content").build();

        when(summaryVectorRepository.findNearestKeywordId(userId, embedding))
            .thenReturn(Optional.of(expectedKeywordId));
        when(knowledgeKeywordRepository.findById(expectedKeywordId)).thenReturn(Optional.of(savedKeyword));
        when(summaryRepository.findById(summaryId)).thenReturn(Optional.of(summary));

        // When
        knowledgeService.addSummaryToSimilarKeyword(userId, summaryId, embedding);

        // Then
        assertThat(summary.getKeyword()).isEqualTo(savedKeyword);
        verify(summaryVectorRepository).updateEmbedding(summaryId, embedding, "test-model");
    }

    @Test
    @DisplayName("유사한 키워드에 요약 추가 테스트 - 키워드가 없는 경우 예외 발생")
    void addSummaryToSimilarKeywordTest_KeywordNotFound() {
        // Given
        Long userId = 1L;
        Long summaryId = 100L;
        List<Double> embedding = Arrays.asList(0.1, 0.2, 0.3);

        when(summaryVectorRepository.findNearestKeywordId(userId, embedding))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> knowledgeService.addSummaryToSimilarKeyword(userId, summaryId, embedding))
            .isInstanceOf(SimilarKeywordNotFoundException.class)
            .hasMessage("유사한 키워드를 찾을 수 없습니다. userId=" + userId);

        verify(summaryVectorRepository).findNearestKeywordId(userId, embedding);
    }

    @Test
    @DisplayName("사용자 지식 트리 조회 테스트")
    void getKnowledgeTreeTest() {
        // Given
        Long userId = 1L;
        List<KnowledgePathDto> mockPaths = Arrays.asList(
            new KnowledgePathDto("Backend", "Database", "MySQL"),
            new KnowledgePathDto("Backend", "Database", "Redis"),
            new KnowledgePathDto("Backend", "Network", "HTTP"),
            new KnowledgePathDto("Frontend", "React", "Hooks")
        );

        when(knowledgeKeywordRepository.findAllKnowledgePathsByUserId(userId)).thenReturn(mockPaths);

        // When
        Map<String, Map<String, List<String>>> result = knowledgeService.getKnowledgeTree(userId);

        // Then
        assertThat(result).containsKeys("Backend", "Frontend");
        
        assertThat(result.get("Backend")).containsKeys("Database", "Network");
        assertThat(result.get("Backend").get("Database")).contains("MySQL", "Redis");
        assertThat(result.get("Backend").get("Network")).contains("HTTP");
        
        assertThat(result.get("Frontend")).containsKeys("React");
        assertThat(result.get("Frontend").get("React")).contains("Hooks");
    }
}
