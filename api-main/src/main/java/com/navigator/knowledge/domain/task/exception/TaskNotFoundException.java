package com.navigator.knowledge.domain.task.exception;

import com.navigator.knowledge.global.exception.BusinessException;
import com.navigator.knowledge.global.exception.ErrorCode;

public class TaskNotFoundException extends BusinessException {

    public TaskNotFoundException(String taskId) {
        super(ErrorCode.TASK_NOT_FOUND, "해당 작업을 찾을 수 없습니다. taskId=" + taskId);
    }
}
