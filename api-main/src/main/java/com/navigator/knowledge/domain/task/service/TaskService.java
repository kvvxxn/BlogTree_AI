package com.navigator.knowledge.domain.task.service;

import com.navigator.knowledge.domain.task.dto.TaskRequestDto;
import com.navigator.knowledge.domain.task.dto.TaskResponseDto;
import com.navigator.knowledge.domain.task.entity.Task;
import com.navigator.knowledge.domain.task.entity.TaskStatus;
import com.navigator.knowledge.domain.task.mq.dto.SummaryTaskRequestMessage;
import com.navigator.knowledge.domain.task.repository.TaskRepository;
import com.navigator.knowledge.domain.tree.service.KnowledgeService;
import com.navigator.knowledge.global.config.rabbitmq.RabbitSummaryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitSummaryProperties rabbitProperties;
    private final KnowledgeService knowledgeService;

    @Transactional
    public TaskResponseDto createTask(TaskRequestDto request) {
        // 1. UUID로 taskId 생성
        String taskId = UUID.randomUUID().toString();


        /**
         * TODO:
         *  - 임시로 사용할 userId (향후 인증 정보에서 가져오도록 수정
         *  - userId UserRepository에서 가져오는걸로 변경
         */
        String userId = "test-user-id";
        Long numericUserId = 1L;

        // 2. Task 엔티티를 PENDING 상태로 DB에 저장
        Task task = Task.builder()
                .taskId(taskId)
                .userId(userId)
                .sourceUrl(request.sourceUrl())
                .status(TaskStatus.PENDING)
                .build();
        
        taskRepository.save(task);

        // 3. 지식 트리 조회 (User의 기존 카테고리, 토픽, 키워드 정보)
        Map<String, Map<String, List<String>>> knowledgeTree = knowledgeService.getKnowledgeTree(numericUserId);

        // 4. RabbitMQ에 보낼 메시지 DTO 생성
        SummaryTaskRequestMessage message = new SummaryTaskRequestMessage(
                taskId,
                userId,
                "Backend Developer", // TODO: 실제 유저의 career_goal 가져오기
                request.sourceUrl(),
                LocalDateTime.now().plusMinutes(30).toString(), // 만료 시간
                knowledgeTree
        );

        // 5. RabbitMQ request 큐에 메시지 발행
        rabbitTemplate.convertAndSend(
                rabbitProperties.exchange(),
                rabbitProperties.routingKey().request(),
                message
        );

        // 메시지 발행 성공 시 Task 상태를 PROCESSING으로 변경
        task.updateStatus(TaskStatus.PROCESSING);

        // 6. TaskResponseDto 반환
        return new TaskResponseDto(taskId);
    }
}
