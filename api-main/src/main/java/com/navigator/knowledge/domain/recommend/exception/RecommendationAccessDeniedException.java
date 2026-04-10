package com.navigator.knowledge.domain.recommend.exception;

import com.navigator.knowledge.global.exception.BusinessException;
import com.navigator.knowledge.global.exception.ErrorCode;

public class RecommendationAccessDeniedException extends BusinessException {

    public RecommendationAccessDeniedException(Long recommendationId, Long userId) {
        super(
                ErrorCode.RECOMMENDATION_ACCESS_DENIED,
                "해당 추천 결과에 접근할 수 없습니다. recommendationId=" + recommendationId + ", userId=" + userId
        );
    }
}
