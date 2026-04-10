package com.navigator.knowledge.domain.task.entity;

public enum TaskStatus {
    PENDING,          // 작업 대기 중 (큐에 들어가기 전)
    PROCESSING,       // 처리 중 (큐에 성공적으로 들어감)
    SUCCESS,          // 작업 성공 (요약, 키워드 추출 모두 성공)
    PARTIAL_SUCCESS,  // 부분 성공 (요약은 성공했지만, 키워드 추출 등 일부 실패)
    FAILED,           // 작업 실패 (스크래핑 차단, LLM 에러 등)
    EXPIRED;          // 작업 만료 (TTL 내 처리 완료되지 않음)

    public boolean isTerminal() {
        return this == SUCCESS || this == PARTIAL_SUCCESS || this == FAILED || this == EXPIRED;
    }
}
