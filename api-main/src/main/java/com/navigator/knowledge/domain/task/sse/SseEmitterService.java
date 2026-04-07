package com.navigator.knowledge.domain.task.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseEmitterService {

    // 기본 타임아웃 5분
    private static final long DEFAULT_TIMEOUT = 5L * 60 * 1000;
    private static final Set<String> TERMINAL_EVENTS = Set.of("success", "partial_success", "failed");

    // todo: Redis pub/sub으로 변경
    // taskId를 키로 하여 SseEmitter를 관리
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, CachedSseEvent> terminalEvents = new ConcurrentHashMap<>();

    /**
     * SseEmitter를 생성하고 연결을 시작합니다.
     */
    public SseEmitter createEmitter(String taskId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        SseEmitter previousEmitter = emitters.put(taskId, emitter);

        log.info("Creating SSE emitter for task: {}", taskId);

        if (previousEmitter != null) {
            log.info("Replacing existing SSE emitter for task: {}", taskId);
            previousEmitter.complete();
        }

        // 연결 종료, 타임아웃, 에러 발생 시 맵에서 제거하는 콜백 등록
        emitter.onCompletion(() -> {
            cleanup(taskId, emitter, "completed", null);
        });
        emitter.onTimeout(() -> {
            cleanup(taskId, emitter, "timed out", null);
            emitter.complete();
        });
        emitter.onError((e) -> {
            cleanup(taskId, emitter, "failed", e);
            emitter.complete();
        });

        try {
            emitter.send(SseEmitter.event()
                .name("connect")
                .data("SSE 연결이 완료되었습니다. 작업 대기 중입니다."));
            log.info("Sent SSE connect event for task: {}", taskId);

            CachedSseEvent terminalEvent = terminalEvents.get(taskId);
            if (terminalEvent != null) {
                log.info("Replaying terminal SSE event for task: {}", taskId);
                emitter.send(SseEmitter.event()
                        .name(terminalEvent.eventName())
                        .data(terminalEvent.data()));
                emitter.complete();
            }
        } catch (IOException e) {
            log.error("Error sending SSE event to task: {}", taskId, e);
            cleanup(taskId, emitter, "failed during initial send", e);
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
            cacheTerminalEvent(taskId, eventName, data);
            log.info("No active SSE emitter found for task: {}. Event cached if terminal.", taskId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data));
            log.info("Sent SSE event. taskId={}, eventName={}", taskId, eventName);
        } catch (IOException e) {
            log.error("Error sending SSE event to task: {}", taskId, e);
            cleanup(taskId, emitter, "failed during event send", e);
            emitter.completeWithError(e);
        }
    }

    /**
     * 특정 작업의 SseEmitter 연결을 정상적으로 종료합니다.
     */
    public void complete(String taskId) {
        SseEmitter emitter = emitters.get(taskId);
        if (emitter != null) {
            log.info("Completing SSE emitter for task: {}", taskId);
            emitter.complete();
        } else {
            log.info("No active SSE emitter to complete for task: {}", taskId);
        }
    }

    private void cleanup(String taskId, SseEmitter emitter, String reason, Throwable throwable) {
        if (throwable == null) {
            log.info("SSE Connection {} for task: {}", reason, taskId);
        } else {
            log.error("SSE Connection {} for task: {}", reason, taskId, throwable);
        }
        emitters.remove(taskId, emitter);
    }

    private void cacheTerminalEvent(String taskId, String eventName, Object data) {
        if (TERMINAL_EVENTS.contains(eventName)) {
            terminalEvents.put(taskId, new CachedSseEvent(eventName, data));
        }
    }
}
