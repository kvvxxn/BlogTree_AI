package com.navigator.knowledge.domain.summary.service;

import com.navigator.knowledge.domain.summary.entity.Summary;
import com.navigator.knowledge.domain.summary.repository.SummaryRepository;
import com.navigator.knowledge.domain.task.entity.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final SummaryRepository summaryRepository;

    @Transactional
    public Summary saveSummary(Task task, Long userId, String sourceUrl, String content) {
        Summary summary = Summary.builder()
                .task(task)
                .userId(userId)
                .sourceUrl(sourceUrl)
                .content(content)
                .build();
        return summaryRepository.save(summary);
    }
}