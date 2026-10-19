package org.datamate.collaboration.exception;

import java.util.Map;

/**
 * Thrown when a requested resource (Thread, Message, Attachment) cannot be found.
 * <p>
 * This is an intent-based, reusable exception — not tied to any specific entity.
 * It maps to HTTP 404 Not Found via {@link org.datamate.collaboration.config.CollaborationExceptionHandler}.
 * <p>
 * Usage:
 * <pre>
 * throw new ResourceNotFoundException(
 *     CollaborationErrorCodes.THREAD_NOT_FOUND.code(), threadId);
 * </pre>
 */
public class ResourceNotFoundException extends CollaborationBaseException {

    public ResourceNotFoundException(String errorCode, Object... messageArgs) {
        super(errorCode, messageArgs);
    }

    public ResourceNotFoundException(String errorCode, String customMessage, Object[] messageArgs, Map<String, Object> metadata) {
        super(errorCode, customMessage, messageArgs, metadata);
    }
}
