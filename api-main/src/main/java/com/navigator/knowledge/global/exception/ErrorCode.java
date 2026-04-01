package com.navigator.knowledge.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    SUMMARY_NOT_FOUND(HttpStatus.NOT_FOUND, "SUMMARY_NOT_FOUND", "해당 요약을 찾을 수 없습니다."),
    SIMILAR_KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "SIMILAR_KEYWORD_NOT_FOUND", "유사한 키워드를 찾을 수 없습니다."),
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "해당 작업을 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String code, String defaultMessage) {
        this.status = status;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
