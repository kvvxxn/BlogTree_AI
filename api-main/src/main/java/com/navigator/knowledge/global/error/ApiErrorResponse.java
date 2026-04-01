package com.navigator.knowledge.global.error;

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
}
