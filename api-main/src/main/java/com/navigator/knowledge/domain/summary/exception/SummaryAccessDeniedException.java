package com.navigator.knowledge.domain.summary.exception;

import com.navigator.knowledge.global.exception.BusinessException;
import com.navigator.knowledge.global.exception.ErrorCode;

public class SummaryAccessDeniedException extends BusinessException {

    public SummaryAccessDeniedException(Long summaryId, Long userId) {
        super(ErrorCode.SUMMARY_ACCESS_DENIED, "해당 요약에 접근할 수 없습니다. summaryId=" + summaryId + ", userId=" + userId);
    }
}
