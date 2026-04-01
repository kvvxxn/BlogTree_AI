package com.navigator.knowledge.domain.tree.exception;

import com.navigator.knowledge.global.exception.BusinessException;
import com.navigator.knowledge.global.exception.ErrorCode;

public class SimilarKeywordNotFoundException extends BusinessException {

    public SimilarKeywordNotFoundException(Long userId) {
        super(ErrorCode.SIMILAR_KEYWORD_NOT_FOUND, "유사한 키워드를 찾을 수 없습니다. userId=" + userId);
    }
}
