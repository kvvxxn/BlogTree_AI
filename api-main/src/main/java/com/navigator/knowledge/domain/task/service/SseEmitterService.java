package com.navigator.knowledge.domain.task.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseEmitterService {

    // 기본 타임아웃 1분
    private static final long DEFAULT_TIMEOUT = 60L * 1000;

    // todo: Redis pub/sub으로 변경
    // taskId를 키로 하여 SseEmitter를 관리
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * SseEmitter를 생성하고 연결을 시작합니다.
     */
    public SseEmitter createEmitter(String taskId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(taskId, emitter);

        // 연결 종료, 타임아웃, 에러 발생 시 맵에서 제거하는 콜백 등록
        emitter.onCompletion(() -> {
            log.info("SSE Connection Completed for task: {}", taskId);
            emitters.remove(taskId, emitter);
        });
        emitter.onTimeout(() -> {
            log.warn("SSE Connection Timeout for task: {}", taskId);
            emitters.remove(taskId, emitter);
        });
        emitter.onError((e) -> {
            log.error("SSE Connection Error for task: {}", taskId, e);
            emitters.remove(taskId, emitter);
        });

        try {
            emitter.send(SseEmitter.event()
                .name("connect")
                .data("SSE 연결이 완료되었습니다. 작업 대기 중입니다."));
        } catch (IOException e) {
            log.error("Error sending SSE event to task: {}", taskId, e);
            emitters.remove(taskId, emitter);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 특정 SseEmitter로 이벤트를 전송합니다.
     */
    public void sendEvent(String taskId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(taskId);
        if (emitter == null) {
            log.warn("No SSE emitter found for task: {}", taskId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data));
        } catch (IOException e) {
            log.error("Error sending SSE event to task: {}", taskId, e);
            emitters.remove(taskId);
            emitter.completeWithError(e);
        }
    }

    /**
     * 특정 작업의 SseEmitter 연결을 정상적으로 종료합니다.
     */
    public void complete(String taskId) {
        SseEmitter emitter = emitters.get(taskId);
        if (emitter != null) {
            emitter.complete();
        }
    }
}
