package com.navigator.knowledge.global.exception;

import com.navigator.knowledge.global.exception.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn(
                "Business exception occurred. code={}, status={}, path={}, message={}",
                errorCode.getCode(),
                errorCode.getStatus().value(),
                request.getRequestURI(),
                e.getMessage()
        );
        return ResponseEntity.status(errorCode.getStatus()).body(ApiErrorResponse.of(
                errorCode,
                e.getMessage(),
                request.getRequestURI()
        ));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception e, HttpServletRequest request) {
        String message = extractMessage(e);
        ErrorCode errorCode = ErrorCode.BAD_REQUEST;
        log.warn(
                "Client error occurred. code={}, status={}, path={}, exceptionType={}, message={}",
                errorCode.getCode(),
                errorCode.getStatus().value(),
                request.getRequestURI(),
                e.getClass().getSimpleName(),
                message
        );
        return ResponseEntity.status(errorCode.getStatus()).body(ApiErrorResponse.of(
                errorCode,
                message,
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Void> handleAsyncRequestTimeout(
            AsyncRequestTimeoutException e,
            HttpServletRequest request
    ) {
        log.warn("Async request timeout occurred. path={}", request.getRequestURI(), e);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleInternalServerError(Exception e, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        log.error(
                "Unhandled exception occurred. code={}, status={}, path={}",
                errorCode.getCode(),
                errorCode.getStatus().value(),
                request.getRequestURI(),
                e
        );
        return ResponseEntity.status(errorCode.getStatus()).body(ApiErrorResponse.of(
                errorCode,
                errorCode.getDefaultMessage(),
                request.getRequestURI()
        ));
    }

    private String extractMessage(Exception e) {
        if (e instanceof MethodArgumentNotValidException validationException) {
            return validationException.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(this::formatFieldError)
                    .collect(Collectors.joining(", "));
        }

        String message = e.getMessage();
        return (message == null || message.isBlank()) ? "잘못된 요청입니다." : message;
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
