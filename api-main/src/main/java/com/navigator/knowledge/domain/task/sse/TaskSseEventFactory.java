package com.navigator.knowledge.domain.task.sse;

import com.navigator.knowledge.domain.task.sse.event.TaskExpiredEvent;
import com.navigator.knowledge.domain.task.sse.event.TaskFailedEvent;
import com.navigator.knowledge.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class TaskSseEventFactory {

    public TaskFailedEvent failed(String taskId, String code, String message) {
        return new TaskFailedEvent(taskId, code, message);
    }

    public TaskFailedEvent failed(String taskId, ErrorCode errorCode, String message) {
        return failed(taskId, errorCode.getCode(), message);
    }

    public TaskExpiredEvent expired(String taskId) {
        return new TaskExpiredEvent(
            taskId,
            ErrorCode.TASK_EXPIRED.getCode(),
            ErrorCode.TASK_EXPIRED.getDefaultMessage()
        );
    }
}
