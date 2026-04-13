package com.navigator.knowledge.domain.task.exception;

import com.navigator.knowledge.global.exception.BusinessException;
import com.navigator.knowledge.global.exception.ErrorCode;

public class TaskAccessDeniedException extends BusinessException {

    public TaskAccessDeniedException(String taskId, Long userId) {
        super(ErrorCode.TASK_ACCESS_DENIED, "해당 작업에 접근할 수 없습니다. taskId=" + taskId + ", userId=" + userId);
    }
}
