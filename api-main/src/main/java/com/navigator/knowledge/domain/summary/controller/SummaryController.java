package com.navigator.knowledge.domain.summary.controller;

import com.navigator.knowledge.domain.summary.dto.SummaryRequestDto;
import com.navigator.knowledge.domain.task.dto.TaskResponseDto;
import com.navigator.knowledge.domain.summary.service.SummaryTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryTaskService summaryTaskService;

    @PostMapping
    public ResponseEntity<TaskResponseDto> requestSummary(@RequestBody SummaryRequestDto request) {
        TaskResponseDto response = summaryTaskService.requestSummary(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}