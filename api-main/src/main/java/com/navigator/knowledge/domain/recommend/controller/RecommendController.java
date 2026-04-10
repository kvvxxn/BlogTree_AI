package com.navigator.knowledge.domain.recommend.controller;

import com.navigator.knowledge.domain.recommend.dto.RecommendationResponseDto;
import com.navigator.knowledge.domain.recommend.service.RecommendTaskService;
import com.navigator.knowledge.domain.recommend.service.RecommendationService;
import com.navigator.knowledge.domain.task.dto.TaskResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendTaskService recommendTaskService;
    private final RecommendationService recommendationService;

    @PostMapping
    public ResponseEntity<TaskResponseDto> requestRecommendation(@AuthenticationPrincipal Long userId) {
        TaskResponseDto response = recommendTaskService.requestRecommendation(userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{recommendationId}")
    public ResponseEntity<RecommendationResponseDto> getRecommendation(@PathVariable Long recommendationId) {
        RecommendationResponseDto response = recommendationService.getRecommendation(recommendationId);
        return ResponseEntity.ok(response);
    }
}
