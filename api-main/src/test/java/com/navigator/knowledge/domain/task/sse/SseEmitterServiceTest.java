package com.navigator.knowledge.domain.task.sse;

import com.navigator.knowledge.domain.task.sse.event.TaskFailedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SseEmitterServiceTest {

    @Test
    @DisplayName("활성 emitter가 없으면 terminal 이벤트를 캐시에 저장한다")
    void publish_cachesTerminalEventWhenNoActiveEmitter() {
        SseEmitterService service = new SseEmitterService();
        TaskFailedEvent event = new TaskFailedEvent("task-1", "FAILED", "error");

        service.publish(event);

        Map<String, CachedSseEvent> terminalEvents = getTerminalEvents(service);
        assertThat(terminalEvents).containsKey("task-1");
    }

    @Test
    @DisplayName("캐시된 terminal 이벤트는 subscribe 시 재생되고 캐시에서 제거된다")
    void createEmitter_replaysCachedTerminalEventAndRemovesIt() {
        SseEmitterService service = new SseEmitterService();
        TaskFailedEvent event = new TaskFailedEvent("task-2", "FAILED", "error");
        service.publish(event);

        assertThat(getTerminalEvents(service)).containsKey("task-2");

        service.createEmitter("task-2");

        assertThat(getTerminalEvents(service)).doesNotContainKey("task-2");
    }

    @Test
    @DisplayName("재생 후 동일 task로 다시 subscribe해도 이전 terminal 이벤트는 남아있지 않다")
    void createEmitter_doesNotReplayRemovedTerminalEventTwice() {
        SseEmitterService service = new SseEmitterService();
        TaskFailedEvent event = new TaskFailedEvent("task-3", "FAILED", "error");
        service.publish(event);

        service.createEmitter("task-3");
        service.createEmitter("task-3");

        assertThat(getTerminalEvents(service)).doesNotContainKey("task-3");
    }

    @SuppressWarnings("unchecked")
    private Map<String, CachedSseEvent> getTerminalEvents(SseEmitterService service) {
        return (Map<String, CachedSseEvent>) ReflectionTestUtils.getField(service, "terminalEvents");
    }
}
