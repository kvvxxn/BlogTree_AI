package com.navigator.knowledge.domain.task.service;

import com.navigator.knowledge.domain.task.entity.Task;
import com.navigator.knowledge.domain.task.sse.SseEmitterService;
import com.navigator.knowledge.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskExpirationScheduler {

    private static final long EXPIRATION_SCAN_INTERVAL_MS = 5_000L;

    private final TaskService taskService;
    private final SseEmitterService sseEmitterService;

    @Scheduled(fixedDelay = EXPIRATION_SCAN_INTERVAL_MS)
    public void expireTimedOutTasks() {
        LocalDateTime now = LocalDateTime.now();
        List<Task> expiredTasks = taskService.expireTimedOutTasks(now);

        for (Task task : expiredTasks) {
            log.info("Expired task after TTL. taskId={}, expiresAt={}", task.getTaskId(), task.getExpiresAt());
            Map<String, Object> sseData = Map.of(
                    "code", ErrorCode.TASK_EXPIRED.getCode(),
                    "message", ErrorCode.TASK_EXPIRED.getDefaultMessage()
            );
            sseEmitterService.sendEvent(task.getTaskId(), "expired", sseData);
            sseEmitterService.complete(task.getTaskId());
        }
    }
}
