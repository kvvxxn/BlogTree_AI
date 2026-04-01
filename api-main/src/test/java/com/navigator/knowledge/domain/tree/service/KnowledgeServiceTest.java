package com.navigator.knowledge.domain.tree.service;

import com.navigator.knowledge.domain.tree.dto.KnowledgePathDto;
import com.navigator.knowledge.domain.tree.repository.KnowledgeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private KnowledgeRepository knowledgeRepository;

    @InjectMocks
    private KnowledgeService knowledgeService;

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

        // When
        knowledgeService.saveKnowledgePath(userId, category, topic, keyword, summaryId, embedding);

        // Then
        verify(knowledgeRepository).addKnowledgeWithSummary(userId, category, topic, keyword, summaryId, embedding);
    }

    @Test
    @DisplayName("유사한 키워드에 요약 추가 테스트 - 키워드가 존재하는 경우")
    void addSummaryToSimilarKeywordTest_KeywordExists() {
        // Given
        Long userId = 1L;
        Long summaryId = 100L;
        List<Double> embedding = Arrays.asList(0.1, 0.2, 0.3);
        Long expectedKeywordId = 10L;

        when(knowledgeRepository.findMostSimilarKeywordId(userId, embedding))
            .thenReturn(Optional.of(expectedKeywordId));

        // When
        knowledgeService.addSummaryToSimilarKeyword(userId, summaryId, embedding);

        // Then
        verify(knowledgeRepository).findMostSimilarKeywordId(userId, embedding);
        verify(knowledgeRepository).createAndAttachSummaryToKeyword(userId, expectedKeywordId, summaryId, embedding);
    }

    @Test
    @DisplayName("유사한 키워드에 요약 추가 테스트 - 키워드가 없는 경우 예외 발생")
    void addSummaryToSimilarKeywordTest_KeywordNotFound() {
        // Given
        Long userId = 1L;
        Long summaryId = 100L;
        List<Double> embedding = Arrays.asList(0.1, 0.2, 0.3);

        when(knowledgeRepository.findMostSimilarKeywordId(userId, embedding))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> knowledgeService.addSummaryToSimilarKeyword(userId, summaryId, embedding))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No such keyword");

        verify(knowledgeRepository).findMostSimilarKeywordId(userId, embedding);
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

        when(knowledgeRepository.findAllKnowledgeByUserId(userId)).thenReturn(mockPaths);

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

    @Test
    @DisplayName("사용자 지식 트리 조회 - Topic이 없는 경우")
    void getKnowledgeTree_NoTopicTest() {
        // Given
        Long userId = 1L;
        List<KnowledgePathDto> mockPaths = Arrays.asList(
            new KnowledgePathDto("Backend", null, null)
        );

        when(knowledgeRepository.findAllKnowledgeByUserId(userId)).thenReturn(mockPaths);

        // When
        Map<String, Map<String, List<String>>> result = knowledgeService.getKnowledgeTree(userId);

        // Then
        assertThat(result).containsKeys("Backend");
        assertThat(result.get("Backend")).containsKeys("No Topic");
        assertThat(result.get("Backend").get("No Topic")).contains("No Keyword");
    }
}
