package com.navigator.knowledge.domain.tree.controller;

import com.navigator.knowledge.domain.tree.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tree")
@RequiredArgsConstructor
public class KnowledgeTreeController {

    private final KnowledgeService knowledgeService;

    // TODO: Principal에서 userId 꺼내기
    @GetMapping
    public ResponseEntity<Map<String, Map<String, List<String>>>> getKnowledgeTree() {
        Map<String, Map<String, List<String>>> knowledgeTree = knowledgeService.getKnowledgeTree(1L);
        return ResponseEntity.ok(knowledgeTree);
    }
}
