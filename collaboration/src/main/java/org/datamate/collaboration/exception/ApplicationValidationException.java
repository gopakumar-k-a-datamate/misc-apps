package org.datamate.collaboration.exception;

import java.util.Map;

/**
 * Thrown when application-level input or use-case command validation fails.
 * <p>
 * In Hexagonal and Clean Architecture, the Application Layer validates incoming
 * use-case commands before executing domain business operations. While
 * {@link DomainValidationException} is strictly reserved for domain entity invariants,
 * {@link ApplicationValidationException} is used for application use-case boundary checks
 * (e.g., null command payloads, missing text/attachments, blocked file extensions, blank URLs).
 * <p>
 * Maps to HTTP 400 Bad Request via {@link org.datamate.collaboration.config.CollaborationExceptionHandler}.
 */
public class ApplicationValidationException extends CollaborationBaseException {

    public ApplicationValidationException(String errorCode, Object... messageArgs) {
        super(errorCode, messageArgs);
    }

    public ApplicationValidationException(String errorCode, String customMessage, Object[] messageArgs) {
        super(errorCode, customMessage, messageArgs);
    }

    public ApplicationValidationException(String errorCode, String customMessage, Object[] messageArgs, Map<String, Object> metadata) {
        super(errorCode, customMessage, messageArgs, metadata);
    }
}