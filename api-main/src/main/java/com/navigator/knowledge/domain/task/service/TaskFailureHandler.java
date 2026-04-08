package com.navigator.knowledge.domain.task.service;

import com.navigator.knowledge.domain.task.sse.SseEmitterService;
import com.navigator.knowledge.domain.task.sse.TaskSseEventFactory;
import com.navigator.knowledge.global.exception.BusinessException;
import com.navigator.knowledge.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskFailureHandler {

    private final TaskService taskService;
    private final SseEmitterService sseEmitterService;
    private final TaskSseEventFactory taskSseEventFactory;

    public void handle(String taskId, String status, BusinessException exception) {
        String errorCode = exception.getErrorCode().getCode();
        String errorMessage = String.format("[%s] %s", errorCode, exception.getMessage());

        log.warn(
                "Failed to process task response. code={}, taskId={}, status={}, message={}",
                errorCode,
                taskId,
                status,
                exception.getMessage(),
                exception
        );

        try {
            taskService.updateTaskFailed(taskId, errorMessage);
        } catch (Exception taskUpdateException) {
            log.error("Failed to mark task as failed. Task ID: {}", taskId, taskUpdateException);
        }

        try {
            sseEmitterService.publish(taskSseEventFactory.failed(taskId, errorCode, exception.getMessage()));
        } catch (Exception sseException) {
            log.error("Failed to send failure SSE event. Task ID: {}", taskId, sseException);
        }
    }

    public void handleUnexpected(String taskId, String status, Exception exception) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        String userMessage = errorCode.getDefaultMessage();
        String errorMessage = String.format("[%s] %s", errorCode.getCode(), userMessage);

        log.error(
                "Unexpected exception while processing task response. code={}, taskId={}, status={}",
                errorCode.getCode(),
                taskId,
                status,
                exception
        );

        try {
            taskService.updateTaskFailed(taskId, errorMessage);
        } catch (Exception taskUpdateException) {
            log.error("Failed to mark task as failed. Task ID: {}", taskId, taskUpdateException);
        }

        try {
            sseEmitterService.publish(taskSseEventFactory.failed(taskId, errorCode, userMessage));
        } catch (Exception sseException) {
            log.error("Failed to send failure SSE event. Task ID: {}", taskId, sseException);
        }
    }
}
