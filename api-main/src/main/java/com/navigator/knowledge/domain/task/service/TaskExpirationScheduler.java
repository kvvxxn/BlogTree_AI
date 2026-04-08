package com.navigator.knowledge.domain.task.service;

import com.navigator.knowledge.domain.task.entity.Task;
import com.navigator.knowledge.domain.task.sse.SseEmitterService;
import com.navigator.knowledge.domain.task.sse.TaskSseEventFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskExpirationScheduler {

    private static final long EXPIRATION_SCAN_INTERVAL_MS = 5_000L;

    private final TaskService taskService;
    private final SseEmitterService sseEmitterService;
    private final TaskSseEventFactory taskSseEventFactory;

    @Scheduled(fixedDelay = EXPIRATION_SCAN_INTERVAL_MS)
    public void expireTimedOutTasks() {
        LocalDateTime now = LocalDateTime.now();
        List<Task> expiredTasks = taskService.expireTimedOutTasks(now);

        for (Task task : expiredTasks) {
            log.info("Expired task after TTL. taskId={}, expiresAt={}", task.getTaskId(), task.getExpiresAt());
            sseEmitterService.publish(taskSseEventFactory.expired(task.getTaskId()));
        }
    }
}
