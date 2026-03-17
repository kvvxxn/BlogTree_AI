package navigator.apimain.domain.tree.service;

import navigator.apimain.domain.tree.dto.KnowledgePathDto;
import navigator.apimain.domain.tree.repository.UserNodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private UserNodeRepository userNodeRepository;

    @InjectMocks
    private KnowledgeService knowledgeService;

    @Test
    @DisplayName("지식 경로 추가 테스트")
    void addKnowledgePathTest() {
        // Given
        Long userId = 1L;
        String category = "Backend";
        String topic = "Database";
        String keyword = "MySQL";

        // When
        knowledgeService.addKnoledgePath(userId, category, topic, keyword);

        // Then
        verify(userNodeRepository).addKnowledge(eq(userId), eq(category), eq(topic), eq(keyword));
    }

    @Test
    @DisplayName("사용자 지식 트리 조회 테스트")
    void getUserKnowledgeTreeTest() {
        // Given
        Long userId = 1L;
        List<KnowledgePathDto> mockPaths = Arrays.asList(
            new KnowledgePathDto("Backend", "Database", "MySQL"),
            new KnowledgePathDto("Backend", "Database", "Redis"),
            new KnowledgePathDto("Backend", "Network", "HTTP"),
            new KnowledgePathDto("Frontend", "React", "Hooks")
        );

        when(userNodeRepository.findAllKnowledgeByUserId(userId)).thenReturn(mockPaths);

        // When
        Map<String, Map<String, List<String>>> result = knowledgeService.getUserKnowledgeTree(userId);

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
    void getUserKnowledgeTree_NoTopicTest() {
        // Given
        Long userId = 1L;
        List<KnowledgePathDto> mockPaths = Arrays.asList(
            new KnowledgePathDto("Backend", null, null)
        );

        when(userNodeRepository.findAllKnowledgeByUserId(userId)).thenReturn(mockPaths);

        // When
        Map<String, Map<String, List<String>>> result = knowledgeService.getUserKnowledgeTree(userId);

        // Then
        assertThat(result).containsKeys("Backend");
        assertThat(result.get("Backend")).containsKeys("No Topic");
        assertThat(result.get("Backend").get("No Topic")).contains("No Keyword");
    }
}
