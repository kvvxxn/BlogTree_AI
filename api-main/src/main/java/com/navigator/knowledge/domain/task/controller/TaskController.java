package com.navigator.knowledge.domain.task.controller;

import com.navigator.knowledge.domain.task.dto.TaskRequestDto;
import com.navigator.knowledge.domain.task.dto.TaskResponseDto;
import com.navigator.knowledge.domain.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponseDto> createTask(@RequestBody TaskRequestDto request) {
        TaskResponseDto response = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
