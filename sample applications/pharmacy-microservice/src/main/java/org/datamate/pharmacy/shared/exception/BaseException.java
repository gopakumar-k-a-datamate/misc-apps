package org.datamate.pharmacy.shared.exception;

import com.datamate.bedrock.framework.common.exception.exceptions.BaseAppException;

/**
 * Shared Base Exception for the microservice.
 * Inherits from the Bedrock framework's BaseAppException to participate in
 * global error handling and RFC 7807 ProblemDetail resolution.
 */
public abstract class BaseException extends BaseAppException {

    protected BaseException(String errorCode) {
        super(errorCode);
    }

    protected BaseException(String errorCode, String defaultMessage) {
        super(errorCode, defaultMessage);
    }

    protected BaseException(String errorCode, String defaultMessage, Object... args) {
        super(errorCode, defaultMessage, args);
    }
}
