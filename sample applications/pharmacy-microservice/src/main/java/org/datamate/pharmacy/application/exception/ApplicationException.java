package org.datamate.pharmacy.application.exception;

import org.datamate.pharmacy.shared.exception.BaseException;

/**
 * Base exception for all application layer (use case) errors.
 * Enforces the use of i18n error codes which the Bedrock MessageResolver
 * converts into localized human-readable messages.
 */
public class ApplicationException extends BaseException {

    public ApplicationException(String errorCode) {
        super(errorCode);
    }

    public ApplicationException(String errorCode, String defaultMessage) {
        super(errorCode, defaultMessage);
    }

    public ApplicationException(String errorCode, String defaultMessage, Object... args) {
        super(errorCode, defaultMessage, args);
    }
}
