package com.navigator.knowledge.domain.summary.messaging.listener;

import com.navigator.knowledge.domain.summary.entity.Summary;
import com.navigator.knowledge.domain.summary.messaging.dto.SummaryTaskResponseMessage;
import com.navigator.knowledge.domain.summary.service.SummaryService;
import com.navigator.knowledge.domain.task.entity.Task;
import com.navigator.knowledge.domain.task.entity.TaskStatus;
import com.navigator.knowledge.domain.task.service.TaskService;
import com.navigator.knowledge.domain.tree.service.KnowledgeService;
import com.navigator.knowledge.global.infra.ai.TextEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryTaskListener {

    private static final String RESPONSE_QUEUE = "summary.response.queue";

    private final TaskService taskService;
    private final SummaryService summaryService;
    private final TextEmbeddingService textEmbeddingService;
    private final KnowledgeService knowledgeService;

    @RabbitListener(queues = RESPONSE_QUEUE)
    public void receiveSummaryResponse(SummaryTaskResponseMessage responseDto) {
        log.info("Receive a summary task result from FastAPI. Task ID: {}, Status: {}",
            responseDto.taskId(), responseDto.status());

        switch (responseDto.status()) {
            case "SUCCESS" -> handleSuccess(responseDto);
            case "PARTIAL_SUCCESS" -> handlePartialSuccess(responseDto);
            case "FAILED" -> handleFailure(responseDto);
            default -> log.warn("Unknown Status In Summary Response: {}", responseDto.status());
        }
    }

    private void handleSuccess(SummaryTaskResponseMessage responseDto) {
        log.info("Handling success for Task ID: {}.", responseDto.taskId());

        var data = responseDto.data();

        // 1. MySQL: Task 상태를 'SUCCESS'로 업데이트
        taskService.updateTaskStatus(responseDto.taskId(), TaskStatus.SUCCESS);
        
        // 2. MySQL: Summary 엔티티 저장
        Task task = taskService.getTask(responseDto.taskId());
        Long userId = responseDto.userId();
        Summary summary = summaryService.saveSummary(
                task,
                userId,
                task.getSourceUrl(),
                data.summaryContent()
        );

        // 3. Embedding: 전달받은 요약 텍스트를 임베딩(Vectorize)
        List<Double> embedding = textEmbeddingService.embedText(data.summaryContent());

        // 4. Neo4j: Category - Topic - Keyword path와 Summary 노드 생성 및 연결
        String category = data.knowledgeTree().category();
        String topic = data.knowledgeTree().topic();
        String keyword = data.knowledgeTree().keyword();

        knowledgeService.saveKnowledgePath(userId, category, topic, keyword, summary.getSummaryId(), embedding);

        log.info("Success handled. Category: {}, Topic: {}, Keywords: {}, Summary ID: {}", category, topic, keyword, summary.getSummaryId());
    }

    private void handlePartialSuccess(SummaryTaskResponseMessage responseDto) {
        log.info("Handling partial success for Task ID: {}.", responseDto.taskId());

        var data = responseDto.data();

        // 1. MySQL: Task 상태를 'PARTIAL_SUCCESS'로 업데이트
        taskService.updateTaskStatus(responseDto.taskId(), TaskStatus.PARTIAL_SUCCESS);

        // 2. MySQL: Summary 엔티티 저장
        Task task = taskService.getTask(responseDto.taskId());
        Long userId = Long.valueOf(responseDto.userId());
        Summary summary = summaryService.saveSummary(
                task,
                userId,
                task.getSourceUrl(),
                data.summaryContent()
        );

        // 3. 요약 텍스트를 임베딩
        List<Double> embedding = textEmbeddingService.embedText(data.summaryContent());

        // 4. Neo4j: 벡터 유사도 검색으로 가장 유사한 Keyword를 찾아 새 Summary 노드를 연결
        knowledgeService.addSummaryToSimilarKeyword(userId, summary.getSummaryId(), embedding);

        log.info("Partial success handled. Summary ID: {} has been linked to the most similar existing keyword.", summary.getSummaryId());
    }
    private void handleFailure(SummaryTaskResponseMessage responseDto) {
        log.info("Handling failed for Task ID: {}.", responseDto.taskId());

        var error = responseDto.error();

        // 1. MySQL: Task 상태를 '실패'로 업데이트하고, 에러 코드와 메시지 기록
        String errorMessage = String.format("[%s] %s", error.code(), error.message());
        taskService.updateTaskFailed(responseDto.taskId(), errorMessage);

        log.error("Task failed. Error Code: {}, Message: {}", error.code(), error.message());
    }
}