package com.navigator.knowledge.domain.summary.service;

import com.navigator.knowledge.domain.summary.dto.SummaryRequestDto;
import com.navigator.knowledge.domain.summary.messaging.dto.SummaryTaskRequestMessage;
import com.navigator.knowledge.domain.summary.messaging.producer.SummaryTaskProducer;
import com.navigator.knowledge.domain.task.dto.TaskResponseDto;
import com.navigator.knowledge.domain.task.entity.Task;
import com.navigator.knowledge.domain.task.entity.TaskStatus;
import com.navigator.knowledge.domain.task.service.TaskService;
import com.navigator.knowledge.domain.tree.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SummaryTaskService {

    private final TaskService taskService;
    private final SummaryTaskProducer summaryTaskProducer;
    private final KnowledgeService knowledgeService;

    @Transactional
    public TaskResponseDto requestSummary(SummaryRequestDto request) {
        // 1. 공통 Task 생성 (DB 저장 완료)
        // 향후 사용자 인증 정보 등에서 가져올 값들
        Long userId = 1L; // TODO: principal에서 가져오기
        Task task = taskService.createTask(userId, request.sourceUrl());

        // 2. 지식 트리 조회
        Map<String, Map<String, List<String>>> knowledgeTree = knowledgeService.getKnowledgeTree(userId);

        // 3. RabbitMQ에 보낼 메시지 DTO 조립
        SummaryTaskRequestMessage message = new SummaryTaskRequestMessage(
                task.getTaskId(),
                userId,
                "Backend Developer", // TODO: 실제 유저의 career_goal 가져오기
                request.sourceUrl(),
                LocalDateTime.now().plusMinutes(5).toString(),
                knowledgeTree
        );

        // 4. 메시지 발행
        summaryTaskProducer.sendTaskRequest(message);

        // 5. 발행 성공 시 상태 업데이트
        taskService.updateTaskStatus(task.getTaskId(), TaskStatus.PROCESSING);

        return new TaskResponseDto(task.getTaskId());
    }
}