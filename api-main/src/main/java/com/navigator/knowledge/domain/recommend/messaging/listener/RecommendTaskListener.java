package com.navigator.knowledge.domain.recommend.messaging.listener;

import com.navigator.knowledge.domain.recommend.entity.Recommendation;
import com.navigator.knowledge.domain.recommend.messaging.dto.RecommendTaskResponseMessage;
import com.navigator.knowledge.domain.recommend.service.RecommendationService;
import com.navigator.knowledge.domain.task.entity.Task;
import com.navigator.knowledge.domain.task.entity.TaskStatus;
import com.navigator.knowledge.domain.task.service.TaskFailureHandler;
import com.navigator.knowledge.domain.task.service.TaskService;
import com.navigator.knowledge.domain.task.sse.SseEmitterService;
import com.navigator.knowledge.global.exception.BusinessException;
import com.navigator.knowledge.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendTaskListener {

    private final TaskService taskService;
    private final RecommendationService recommendationService;
    private final SseEmitterService sseEmitterService;
    private final TaskFailureHandler taskFailureHandler;

    @RabbitListener(queues = "${spring.rabbitmq.recommend.queue.response}")
    public void receiveRecommendResponse(RecommendTaskResponseMessage responseMessage) {
        log.info("Receive a recommend task result from FastAPI. Task ID: {}, Status: {}",
            responseMessage.taskId(), responseMessage.status());

        try {
            process(responseMessage);
        } catch (BusinessException e) {
            taskFailureHandler.handle(responseMessage.taskId(), responseMessage.status(), e);
        } catch (Exception e) {
            taskFailureHandler.handleUnexpected(responseMessage.taskId(), responseMessage.status(), e);
        }
    }

    private void process(RecommendTaskResponseMessage responseMessage) {
        String status = normalizeStatus(responseMessage.status());
        validateKnownStatus(status, responseMessage.status());

        Task task = taskService.getTask(responseMessage.taskId());
        if (!isProcessable(task, responseMessage)) {
            return;
        }

        switch (status) {
            case "SUCCESS" -> handleSuccess(task, responseMessage);
            case "FAILED" -> handleFailure(responseMessage);
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Unknown status in recommend response: " + responseMessage.status());
        }
    }

    private void validateKnownStatus(String normalizedStatus, String originalStatus) {
        if (!List.of("SUCCESS", "FAILED").contains(normalizedStatus)) {
            throw new BusinessException(
                ErrorCode.BAD_REQUEST,
                "Unknown status in recommend response: " + originalStatus
            );
        }
    }

    private boolean isProcessable(Task task, RecommendTaskResponseMessage responseMessage) {
        if (task.getStatus().isTerminal()) {
            log.info("Ignoring recommend response for terminal task. taskId={}, status={}", task.getTaskId(), task.getStatus());
            return false;
        }

        if (task.isExpiredAt(LocalDateTime.now())) {
            log.warn("Discarding stale recommend response after TTL. taskId={}, responseStatus={}, expiresAt={}",
                task.getTaskId(),
                responseMessage.status(),
                task.getExpiresAt());

            if (taskService.expireTask(task.getTaskId())) {
                Map<String, Object> sseData = Map.of(
                    "code", ErrorCode.TASK_EXPIRED.getCode(),
                    "message", ErrorCode.TASK_EXPIRED.getDefaultMessage()
                );
                sseEmitterService.sendEvent(task.getTaskId(), "expired", sseData);
                sseEmitterService.complete(task.getTaskId());
            }
            return false;
        }

        return true;
    }

    private void handleSuccess(Task task, RecommendTaskResponseMessage responseMessage) {
        RecommendTaskResponseMessage.ResultData data = requireResultData(responseMessage);
        Long userId = requireUserId(responseMessage);

        Recommendation recommendation = recommendationService.findOrCreateRecommendation(
            task,
            userId,
            requireText(data.reason(), "reason"),
            requireText(data.category(), "category"),
            requireText(data.topic(), "topic"),
            requireText(data.keyword(), "keyword")
        );

        taskService.updateTaskStatus(task.getTaskId(), TaskStatus.SUCCESS);

        Map<String, Object> sseData = new HashMap<>();
        sseData.put("reason", recommendation.getReason());
        sseData.put("category", recommendation.getCategory());
        sseData.put("topic", recommendation.getTopic());
        sseData.put("keyword", recommendation.getKeyword());
        if (recommendation.getRecommendationId() != null) {
            sseData.put("recommendationId", recommendation.getRecommendationId());
        }
        sseEmitterService.sendEvent(task.getTaskId(), "success", sseData);
        sseEmitterService.complete(task.getTaskId());

        log.info("Recommend success handled. Recommendation ID: {}", recommendation.getRecommendationId());
    }

    private void handleFailure(RecommendTaskResponseMessage responseMessage) {
        RecommendTaskResponseMessage.ErrorData error = requireErrorData(responseMessage);

        String errorMessage = String.format("[%s] %s", error.code(), error.message());
        taskService.updateTaskFailed(responseMessage.taskId(), errorMessage);

        Map<String, Object> sseData = Map.of(
            "code", error.code(),
            "message", error.message()
        );
        sseEmitterService.sendEvent(responseMessage.taskId(), "failed", sseData);
        sseEmitterService.complete(responseMessage.taskId());

        log.error("Recommend task failed handled. Error Code: {}, Message: {}", error.code(), error.message());
    }

    private String normalizeStatus(String status) {
        return requireText(status, "status");
    }

    private Long requireUserId(RecommendTaskResponseMessage responseMessage) {
        if (responseMessage.userId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "userId must not be null");
        }
        return responseMessage.userId();
    }

    private RecommendTaskResponseMessage.ResultData requireResultData(RecommendTaskResponseMessage responseMessage) {
        if (responseMessage.data() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "data must not be null");
        }
        return responseMessage.data();
    }

    private RecommendTaskResponseMessage.ErrorData requireErrorData(RecommendTaskResponseMessage responseMessage) {
        if (responseMessage.error() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "error must not be null");
        }

        requireText(responseMessage.error().code(), "error.code");
        requireText(responseMessage.error().message(), "error.message");
        return responseMessage.error();
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + " must not be blank");
        }
        return value;
    }
}
