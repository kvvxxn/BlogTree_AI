package com.navigator.knowledge.domain.summary.messaging.listener;

import com.navigator.knowledge.domain.summary.entity.Summary;
import com.navigator.knowledge.domain.summary.messaging.dto.SummaryTaskResponseMessage;
import com.navigator.knowledge.domain.summary.service.SummaryService;
import com.navigator.knowledge.domain.task.entity.Task;
import com.navigator.knowledge.domain.task.entity.TaskStatus;
import com.navigator.knowledge.domain.task.service.SseEmitterService;
import com.navigator.knowledge.domain.task.service.TaskService;
import com.navigator.knowledge.domain.tree.service.KnowledgeService;
import com.navigator.knowledge.global.infra.ai.TextEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryTaskListener {

    private static final String RESPONSE_QUEUE = "summary.response.queue";

    private final TaskService taskService;
    private final SummaryService summaryService;
    private final TextEmbeddingService textEmbeddingService;
    private final KnowledgeService knowledgeService;
    private final SseEmitterService sseEmitterService;

    @RabbitListener(queues = RESPONSE_QUEUE)
    public void receiveSummaryResponse(SummaryTaskResponseMessage responseDto) {
        log.info("Receive a summary task result from FastAPI. Task ID: {}, Status: {}",
            responseDto.taskId(), responseDto.status());

        try {
            switch (normalizeStatus(responseDto.status())) {
                case "SUCCESS" -> handleSuccess(responseDto);
                case "PARTIAL_SUCCESS" -> handlePartialSuccess(responseDto);
                case "FAILED" -> handleFailure(responseDto);
                default -> throw new IllegalArgumentException("Unknown status in summary response: " + responseDto.status());
            }
        } catch (Exception e) {
            handleProcessingError(responseDto, e);
        }
    }

    private void handleSuccess(SummaryTaskResponseMessage responseDto) {
        var data = requireResultData(responseDto);
        Task task = taskService.getTask(responseDto.taskId());
        Long userId = requireUserId(responseDto);
        Summary summary = summaryService.findOrCreateSummary(
                task,
                userId,
                task.getSourceUrl(),
                data.summaryContent()
        );

        List<Double> embedding = textEmbeddingService.embedText(data.summaryContent());

        var knowledgeTree = requireKnowledgeTree(data);
        String category = requireText(knowledgeTree.category(), "category");
        String topic = requireText(knowledgeTree.topic(), "topic");
        String keyword = requireText(knowledgeTree.keyword(), "keyword");

        knowledgeService.saveKnowledgePath(userId, category, topic, keyword, summary.getSummaryId(), embedding);

        taskService.updateTaskStatus(responseDto.taskId(), TaskStatus.SUCCESS);

        Map<String, Object> sseData = Map.of(
            "category", category,
            "topic", topic,
            "keyword", keyword,
            "summaryContent", data.summaryContent()
        );
        sseEmitterService.sendEvent(responseDto.taskId(), "success", sseData);
        sseEmitterService.complete(responseDto.taskId());

        log.info("Success handled. Category: {}, Topic: {}, Keywords: {}, Summary ID: {}", category, topic, keyword, summary.getSummaryId());
    }

    private void handlePartialSuccess(SummaryTaskResponseMessage responseDto) {
        var data = requireResultData(responseDto);
        Task task = taskService.getTask(responseDto.taskId());
        Long userId = requireUserId(responseDto);
        Summary summary = summaryService.findOrCreateSummary(
                task,
                userId,
                task.getSourceUrl(),
                data.summaryContent()
        );

        List<Double> embedding = textEmbeddingService.embedText(data.summaryContent());

        knowledgeService.addSummaryToSimilarKeyword(userId, summary.getSummaryId(), embedding);

        var knowledgeTree = requireKnowledgeTree(data);
        taskService.updateTaskStatus(responseDto.taskId(), TaskStatus.PARTIAL_SUCCESS);

        Map<String, Object> sseData = Map.of(
            "category", requireText(knowledgeTree.category(), "category"),
            "topic", requireText(knowledgeTree.topic(), "topic"),
            "keyword", requireText(knowledgeTree.keyword(), "keyword"),
            "summaryContent", data.summaryContent()
        );
        sseEmitterService.sendEvent(responseDto.taskId(), "partial_success", sseData);
        sseEmitterService.complete(responseDto.taskId());

        log.info("Partial success handled. Summary ID: {} has been linked to the most similar existing keyword.", summary.getSummaryId());
    }
    
    private void handleFailure(SummaryTaskResponseMessage responseDto) {
        var error = requireErrorData(responseDto);

        String errorMessage = String.format("[%s] %s", error.code(), error.message());
        taskService.updateTaskFailed(responseDto.taskId(), errorMessage);

        Map<String, Object> sseData = Map.of(
            "code", error.code(),
            "message", error.message()
        );
        sseEmitterService.sendEvent(responseDto.taskId(), "failed", sseData);
        sseEmitterService.complete(responseDto.taskId());

        log.error("Task failed handled. Error Code: {}, Message: {}", error.code(), error.message());
    }

    private void handleProcessingError(SummaryTaskResponseMessage responseDto, Exception e) {
        String taskId = responseDto.taskId();
        String errorMessage = String.format("[LISTENER_PROCESSING_ERROR] %s", e.getMessage());

        log.error("Failed to process summary response. Task ID: {}, Status: {}", taskId, responseDto.status(), e);

        try {
            taskService.updateTaskFailed(taskId, errorMessage);
        } catch (Exception taskUpdateException) {
            log.error("Failed to mark task as failed. Task ID: {}", taskId, taskUpdateException);
        }

        try {
            Map<String, Object> sseData = Map.of(
                "code", "LISTENER_PROCESSING_ERROR",
                "message", Objects.requireNonNullElse(e.getMessage(), "Failed to process summary response")
            );
            sseEmitterService.sendEvent(taskId, "failed", sseData);
            sseEmitterService.complete(taskId);
        } catch (Exception sseException) {
            log.error("Failed to send failure SSE event. Task ID: {}", taskId, sseException);
        }
    }

    private String normalizeStatus(String status) {
        return requireText(status, "status");
    }

    private Long requireUserId(SummaryTaskResponseMessage responseDto) {
        if (responseDto.userId() == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        return responseDto.userId();
    }

    private SummaryTaskResponseMessage.ResultData requireResultData(SummaryTaskResponseMessage responseDto) {
        if (responseDto.data() == null) {
            throw new IllegalArgumentException("data must not be null");
        }

        requireText(responseDto.data().summaryContent(), "summaryContent");
        return responseDto.data();
    }

    private SummaryTaskResponseMessage.KnowledgeTree requireKnowledgeTree(SummaryTaskResponseMessage.ResultData data) {
        if (data.knowledgeTree() == null) {
            throw new IllegalArgumentException("knowledgeTree must not be null");
        }
        return data.knowledgeTree();
    }

    private SummaryTaskResponseMessage.ErrorData requireErrorData(SummaryTaskResponseMessage responseDto) {
        if (responseDto.error() == null) {
            throw new IllegalArgumentException("error must not be null");
        }

        requireText(responseDto.error().code(), "error.code");
        requireText(responseDto.error().message(), "error.message");
        return responseDto.error();
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
