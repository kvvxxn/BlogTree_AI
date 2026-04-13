package com.navigator.knowledge.domain.user.exception;

import com.navigator.knowledge.global.exception.BusinessException;
import com.navigator.knowledge.global.exception.ErrorCode;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(Long userId) {
        super(ErrorCode.USER_NOT_FOUND, "해당 사용자를 찾을 수 없습니다. userId=" + userId);
    }
}
