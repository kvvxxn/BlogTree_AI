package com.navigator.knowledge.domain.task.controller;

import com.navigator.knowledge.domain.task.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final SseEmitterService sseEmitterService;

    // 요약, 추천 등 모든 작업의 진행 상태를 구독하는 단일 엔드포인트
    @GetMapping(value = "/subscribe/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(@PathVariable String taskId) {
        SseEmitter emitter = sseEmitterService.createEmitter(taskId);
        return ResponseEntity.ok(emitter);
    }
}