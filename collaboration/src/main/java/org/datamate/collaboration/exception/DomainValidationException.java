package org.datamate.collaboration.exception;

import java.util.Map;

/**
 * Thrown when a domain invariant or input validation constraint is violated.
 * <p>
 * This is an intent-based, reusable exception for all validation failures:
 * <ul>
 *   <li>Required fields missing (null/blank)</li>
 *   <li>Value out of allowed range</li>
 *   <li>Format constraint violations</li>
 * </ul>
 * It maps to HTTP 400 Bad Request via {@link org.datamate.collaboration.config.CollaborationExceptionHandler}.
 * <p>
 * Usage:
 * <pre>
 * throw new DomainValidationException(
 *     CollaborationErrorCodes.MESSAGE_TEXT_BLANK.code(), "text");
 * </pre>
 */
public class DomainValidationException extends CollaborationBaseException {

    public DomainValidationException(String errorCode, Object... messageArgs) {
        super(errorCode, messageArgs);
    }

    public DomainValidationException(String errorCode, String customMessage, Object[] messageArgs, Map<String, Object> metadata) {
        super(errorCode, customMessage, messageArgs, metadata);
    }
}
