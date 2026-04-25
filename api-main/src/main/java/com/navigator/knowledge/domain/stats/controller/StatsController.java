package com.navigator.knowledge.domain.stats.controller;

import com.navigator.knowledge.domain.stats.dto.StatsResponseDto;
import com.navigator.knowledge.domain.stats.service.StatsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class StatsController {

    private final StatsService statsService;

    @GetMapping
    public ResponseEntity<StatsResponseDto> getStats(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(statsService.getStats(userId));
    }
}
