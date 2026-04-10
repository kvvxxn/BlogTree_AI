package com.navigator.knowledge.domain.task.controller;

import com.navigator.knowledge.domain.task.sse.SseEmitterService;
import com.navigator.knowledge.domain.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final SseEmitterService sseEmitterService;
    private final TaskService taskService;

    // 요약, 추천 등 모든 작업의 진행 상태를 구독하는 단일 엔드포인트
    @GetMapping(value = "/subscribe/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(
            @AuthenticationPrincipal Long userId,
            @PathVariable String taskId
    ) {
        taskService.getOwnedTask(userId, taskId);
        SseEmitter emitter = sseEmitterService.createEmitter(taskId);
        return ResponseEntity.ok(emitter);
    }
}
