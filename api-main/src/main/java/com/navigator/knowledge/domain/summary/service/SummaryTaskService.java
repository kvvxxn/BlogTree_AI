package com.navigator.knowledge.domain.summary.service;

import com.navigator.knowledge.domain.summary.dto.SummaryRequestDto;
import com.navigator.knowledge.domain.summary.messaging.dto.SummaryTaskRequestMessage;
import com.navigator.knowledge.domain.summary.messaging.producer.SummaryTaskProducer;
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
public class SummaryTaskService {

    private static final String JPA_TRANSACTION_MANAGER = "jpaTransactionManager";
    private static final long TASK_TTL_SECONDS = 45L;

    private final TaskService taskService;
    private final SummaryTaskProducer summaryTaskProducer;
    private final KnowledgeService knowledgeService;
    private final UserService userService;

    @Transactional(transactionManager = JPA_TRANSACTION_MANAGER)
    public TaskResponseDto requestSummary(Long userId, SummaryRequestDto request) {
        // 1. 공통 Task 생성 (DB 저장 완료)
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(TASK_TTL_SECONDS);
        Task task = taskService.createTask(userId, request.sourceUrl(), TaskType.SUMMARY, expiresAt);

        // 2. 지식 트리 조회
        Map<String, Map<String, List<String>>> knowledgeTree = knowledgeService.getKnowledgeTree(userId);
        String careerGoal = userService.getRequiredCareerGoal(userId);

        // 3. RabbitMQ에 보낼 메시지 DTO 조립
        SummaryTaskRequestMessage message = new SummaryTaskRequestMessage(
                task.getTaskId(),
                userId,
                careerGoal,
                request.sourceUrl(),
                expiresAt.toString(),
                knowledgeTree
        );

        // 4. 메시지 발행
        summaryTaskProducer.sendTaskRequest(message);

        // 5. 발행 성공 시 상태 업데이트
        taskService.updateTaskStatus(task.getTaskId(), TaskStatus.PROCESSING);

        return new TaskResponseDto(task.getTaskId());
    }
}
