package com.navigator.knowledge.global.security.oauth2.exception;

import com.navigator.knowledge.global.exception.BusinessException;
import com.navigator.knowledge.global.exception.ErrorCode;

public class GoogleOAuthException extends BusinessException {

    public GoogleOAuthException() {
        super(ErrorCode.GOOGLE_OAUTH_FAILED);
    }

    public GoogleOAuthException(String message) {
        super(ErrorCode.GOOGLE_OAUTH_FAILED, message);
    }

    public GoogleOAuthException(String message, Throwable cause) {
        super(ErrorCode.GOOGLE_OAUTH_FAILED, message, cause);
    }
}
