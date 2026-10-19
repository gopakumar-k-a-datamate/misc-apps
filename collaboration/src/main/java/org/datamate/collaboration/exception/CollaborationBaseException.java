package org.datamate.collaboration.exception;

import com.datamate.bedrock.framework.common.exception.exceptions.BaseAppException;

import java.util.Map;

/**
 * Base exception for the entire Collaboration Microservice.
 * <p>
 * All collaboration-specific exceptions must extend this class.
 * Extends Bedrock's {@link BaseAppException} to integrate with the framework's
 * centralized exception handling, error codes, i18n, and
 * {@link com.datamate.bedrock.framework.common.exception.spring.service.web.GlobalExceptionHandler}.
 * <p>
 * Error codes for this service follow the convention: {@code COLLAB-[MODULE]-[NUMBER]}.
 */
public class CollaborationBaseException extends BaseAppException {

    public CollaborationBaseException(String errorCode, Object... messageArgs) {
        super(errorCode, messageArgs);
    }

    public CollaborationBaseException(String errorCode, String customMessage, Object[] messageArgs) {
        super(errorCode, customMessage, messageArgs);
    }

    public CollaborationBaseException(String errorCode, String customMessage, Object[] messageArgs, Map<String, Object> metadata) {
        super(errorCode, customMessage, messageArgs, metadata);
    }

    public CollaborationBaseException(String errorCode, String customMessage, Object[] messageArgs, Map<String, Object> metadata, Throwable cause) {
        super(errorCode, customMessage, messageArgs, metadata, cause);
    }
}
