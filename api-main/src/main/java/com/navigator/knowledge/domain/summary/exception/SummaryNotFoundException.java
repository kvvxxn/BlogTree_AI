package com.navigator.knowledge.domain.summary.exception;

public class SummaryNotFoundException extends RuntimeException {

    public SummaryNotFoundException(Long summaryId) {
        super("해당 요약을 찾을 수 없습니다. summaryId=" + summaryId);
    }
}
