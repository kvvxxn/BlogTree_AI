package com.navigator.knowledge.domain.task.entity;

public enum TaskStatus {
    PENDING,          // 작업 대기 중 (큐에 들어간 상태)
    SUCCESS,          // 작업 성공 (요약, 키워드 추출 모두 성공)
    PARTIAL_SUCCESS,  // 부분 성공 (요약은 성공했지만, 키워드 추출 등 일부 실패)
    FAILED            // 작업 실패 (스크래핑 차단, LLM 에러 등)
}
