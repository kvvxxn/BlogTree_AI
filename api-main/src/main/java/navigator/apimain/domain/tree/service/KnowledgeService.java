package navigator.apimain.domain.tree.service;

import lombok.RequiredArgsConstructor;
import navigator.apimain.domain.tree.dto.KnowledgePathDto;
import navigator.apimain.domain.tree.repository.UserNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeService {
    private final UserNodeRepository userNodeRepository;

    @Transactional
    public void addKnoledgePath(Long userId, String categoryName, String topicName, String keywordName) {
        userNodeRepository.addKnowledge(userId, categoryName, topicName, keywordName);
    }

    public Map<String, Map<String, List<String>>> getUserKnowledgeTree(Long userId) {
        List<KnowledgePathDto> paths = userNodeRepository.findAllKnowledgeByUserId(userId);

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
