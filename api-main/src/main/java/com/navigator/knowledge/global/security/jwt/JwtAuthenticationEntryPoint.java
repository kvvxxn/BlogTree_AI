package com.navigator.knowledge.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navigator.knowledge.global.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        // 1. 아까 우리 문지기(Filter)가 request에 남겨둔 에러 메모를 읽어옵니다.
        // (예: "EXPIRED_TOKEN", "INVALID_TOKEN" 등)
        String exception = (String) request.getAttribute("exception");

        // 만약 메모가 없다면 그냥 일반적인 "인증되지 않은 사용자" 에러로 처리
        ErrorCode errorCode;
        try {
            errorCode = ErrorCode.valueOf(exception);
        } catch (Exception e) {
            errorCode = ErrorCode.UNAUTHORIZED;
        }


        log.error("인증 실패: {}", errorCode.getDefaultMessage());

        // 2. 프론트엔드가 읽기 편하게 JSON 형태로 세팅
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 상태 코드

        // 3. 응답 바디에 넣을 데이터 (Map을 써서 JSON처럼 만듦)
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", errorCode.getStatus().value());
        responseBody.put("error", errorCode.getCode());
        responseBody.put("message", errorCode.getDefaultMessage());
        responseBody.put("path", request.getRequestURI());
        // 4. ObjectMapper를 써서 Map을 진짜 JSON 문자열로 바꿔서 프론트에게 쏩니다
        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
    }

    // 상황별 메시지를 정해주는 간단한 헬퍼 메서드
    private String getErrorMessage(String errorCode) {
        return switch (errorCode) {
            case "EXPIRED_TOKEN" -> "토큰이 만료되었습니다. 다시 로그인하거나 재발급받으세요.";
            case "INVALID_TOKEN" -> "유효하지 않은 토큰입니다. 다시 확인해주세요.";
            case "INVALID_TOKEN_TYPE" -> "토큰 타입이 올바르지 않습니다. (Access Token 필요)";
            case "UNAUTHORIZED" -> "인증 정보가 없습니다. 로그인이 필요합니다.";
            default -> "인증에 실패했습니다. 유효한 토큰을 제공해주세요.";
        };
    }
}
