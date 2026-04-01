package com.navigator.knowledge.domain.summary.exception;

import com.navigator.knowledge.global.exception.BusinessException;
import com.navigator.knowledge.global.exception.ErrorCode;

public class SummaryNotFoundException extends BusinessException {

    public SummaryNotFoundException(Long summaryId) {
        super(ErrorCode.SUMMARY_NOT_FOUND, "해당 요약을 찾을 수 없습니다. summaryId=" + summaryId);
    }
}
