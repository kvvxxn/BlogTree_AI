package com.navigator.knowledge.domain.task.sse;

import com.navigator.knowledge.domain.task.sse.event.TaskSseEvent;
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
    private static final long DEFAULT_TIMEOUT = (long) 60 * 1000;

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

            replayTerminalEventIfPresent(taskId, emitter);
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
    public void publish(TaskSseEvent event) {
        SseEmitter emitter = emitters.get(event.taskId());

        if (emitter == null) {
            cacheTerminalEvent(event);
            log.info("No active SSE emitter found for task: {}. Event cached if terminal.", event.taskId());
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                .name(event.eventName().value())
                .data(event.payload()));
            log.info("Sent SSE event. taskId={}, eventName={}", event.taskId(), event.eventName().value());

            if (event.terminal()) {
                log.info("Completing SSE emitter for task: {}", event.taskId());
                emitter.complete();
            }
        } catch (IOException e) {
            log.error("Error sending SSE event to task: {}", event.taskId(), e);
            cleanup(event.taskId(), emitter, "failed during event send", e);
            emitter.completeWithError(e);
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

    private void cacheTerminalEvent(TaskSseEvent event) {
        if (event.terminal()) {
            terminalEvents.put(event.taskId(), new CachedSseEvent(event));
        }
    }

    private void replayTerminalEventIfPresent(String taskId, SseEmitter emitter) throws IOException {
        CachedSseEvent terminalEvent = terminalEvents.remove(taskId);
        if (terminalEvent == null) {
            return;
        }

        log.info("Replaying terminal SSE event for task: {}", taskId);
        emitter.send(SseEmitter.event()
            .name(terminalEvent.event().eventName().value())
            .data(terminalEvent.event().payload()));
        emitter.complete();
    }
}
