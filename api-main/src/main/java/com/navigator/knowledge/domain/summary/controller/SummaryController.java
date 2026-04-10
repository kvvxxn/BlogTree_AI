package com.navigator.knowledge.domain.summary.controller;

import com.navigator.knowledge.domain.summary.dto.SummaryRequestDto;
import com.navigator.knowledge.domain.summary.dto.SummaryResponseDto;
import com.navigator.knowledge.domain.summary.service.SummaryService;
import com.navigator.knowledge.domain.task.dto.TaskResponseDto;
import com.navigator.knowledge.domain.summary.service.SummaryTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryTaskService summaryTaskService;
    private final SummaryService summaryService;

    @PostMapping
    public ResponseEntity<TaskResponseDto> requestSummary(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SummaryRequestDto request
    ) {
        TaskResponseDto response = summaryTaskService.requestSummary(userId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    @GetMapping("/{summaryId}")
    public ResponseEntity<SummaryResponseDto> getSummary(
            @AuthenticationPrincipal Long userId,
            @PathVariable("summaryId") Long summaryId
    ) {
        SummaryResponseDto response = summaryService.getSummary(userId, summaryId);
        return ResponseEntity.ok(response);
    }
}
