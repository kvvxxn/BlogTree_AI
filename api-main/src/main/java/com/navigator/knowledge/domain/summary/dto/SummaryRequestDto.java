package com.navigator.knowledge.domain.summary.dto;

import jakarta.validation.constraints.NotBlank;

public record SummaryRequestDto(
    @NotBlank(message = "sourceUrl은 비어 있을 수 없습니다.")
    String sourceUrl
) {}
