package com.navigator.knowledge.domain.tree.controller;

import com.navigator.knowledge.domain.tree.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tree")
@RequiredArgsConstructor
public class KnowledgeTreeController {

    private final KnowledgeService knowledgeService;

    @GetMapping
    public ResponseEntity<Map<String, Map<String, List<String>>>> getKnowledgeTree(
            @AuthenticationPrincipal Long userId
    ) {
        Map<String, Map<String, List<String>>> knowledgeTree = knowledgeService.getKnowledgeTree(userId);
        return ResponseEntity.ok(knowledgeTree);
    }
}
