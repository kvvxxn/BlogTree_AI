package com.navigator.knowledge.domain.recommend.exception;

import com.navigator.knowledge.global.exception.BusinessException;
import com.navigator.knowledge.global.exception.ErrorCode;

public class RecommendationNotFoundException extends BusinessException {

    public RecommendationNotFoundException(Long recommendationId) {
        super(ErrorCode.RECOMMENDATION_NOT_FOUND, "해당 추천 결과를 찾을 수 없습니다. recommendationId=" + recommendationId);
    }
}
