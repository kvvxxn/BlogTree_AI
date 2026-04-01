package com.navigator.knowledge.global.exception.dto;

import com.navigator.knowledge.global.exception.ErrorCode;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String code,
        String message,
        String path
) {
    public static ApiErrorResponse of(int status, String code, String message, String path) {
        return new ApiErrorResponse(LocalDateTime.now(), status, code, message, path);
    }

    public static ApiErrorResponse of(ErrorCode errorCode, String message, String path) {
        return of(
                errorCode.getStatus().value(),
                errorCode.getCode(),
                message,
                path
        );
    }
}
