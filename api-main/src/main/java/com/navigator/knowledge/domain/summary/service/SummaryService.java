package com.navigator.knowledge.domain.summary.service;

import com.navigator.knowledge.domain.summary.dto.SummaryResponseDto;
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

    @Transactional(readOnly = true)
    public SummaryResponseDto getSummary(Long summaryId) {
        Summary summary = summaryRepository.findById(summaryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 요약을 찾을 수 없습니다. summaryId=" + summaryId));
        return SummaryResponseDto.from(summary);
    }
}