package com.navigator.knowledge.domain.recommend.service;

import com.navigator.knowledge.domain.recommend.messaging.dto.RecommendTaskRequestMessage;
import com.navigator.knowledge.domain.recommend.messaging.producer.RecommendTaskProducer;
import com.navigator.knowledge.domain.task.dto.TaskResponseDto;
import com.navigator.knowledge.domain.task.entity.Task;
import com.navigator.knowledge.domain.task.entity.TaskStatus;
import com.navigator.knowledge.domain.task.entity.TaskType;
import com.navigator.knowledge.domain.task.service.TaskService;
import com.navigator.knowledge.domain.tree.service.KnowledgeService;
import com.navigator.knowledge.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecommendTaskService {

    private static final String JPA_TRANSACTION_MANAGER = "jpaTransactionManager";
    private static final long TASK_TTL_SECONDS = 45L;

    private final TaskService taskService;
    private final RecommendTaskProducer recommendTaskProducer;
    private final KnowledgeService knowledgeService;
    private final UserService userService;

    @Transactional(transactionManager = JPA_TRANSACTION_MANAGER)
    public TaskResponseDto requestRecommendation(Long userId) {
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(TASK_TTL_SECONDS);
        Task task = taskService.createTask(userId, null, TaskType.RECOMMEND, expiresAt);

        Map<String, Map<String, List<String>>> knowledgeTree = knowledgeService.getKnowledgeTree(userId);
        String careerGoal = userService.getRequiredCareerGoal(userId);

        RecommendTaskRequestMessage message = new RecommendTaskRequestMessage(
            task.getTaskId(),
            userId,
            careerGoal,
            expiresAt.toString(),
            knowledgeTree
        );

        recommendTaskProducer.sendTaskRequest(message);
        taskService.updateTaskStatus(task.getTaskId(), TaskStatus.PROCESSING);

        return new TaskResponseDto(task.getTaskId());
    }
}
