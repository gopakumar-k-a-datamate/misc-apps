package org.datamate.collaboration.exception;

/**
 * Centralized error codes for the Collaboration Microservice.
 * <p>
 * Follows the Bedrock convention: {@code COLLAB-[MODULE]-[NUMBER]}.
 * <ul>
 *   <li>{@code COLLAB-VAL} - Domain validation / invariant violations</li>
 *   <li>{@code COLLAB-CHT} - Chat module</li>
 *   <li>{@code COLLAB-ATT} - Attachment module</li>
 *   <li>{@code COLLAB-SEC} - Security / authorization module</li>
 * </ul>
 * <p>
 * These codes are resolved against Spring's {@code MessageSource} for
 * internationalized error messages by Bedrock's
 * {@link com.datamate.bedrock.framework.common.exception.spring.service.web.GlobalExceptionHandler}.
 */
public enum CollaborationErrorCodes {

    // ========== Validation (VAL) - Reusable domain invariant violations ==========

    /** A required field is null. Args: fieldName. */
    REQUIRED_FIELD_MISSING("COLLAB-VAL-001"),

    /** A required text field is blank or empty. Args: fieldName. */
    FIELD_BLANK("COLLAB-VAL-002"),

    /** A numeric value is out of the allowed range. Args: fieldName, min, max. */
    VALUE_OUT_OF_RANGE("COLLAB-VAL-003"),

    // ========== Chat Module (CHT) ==========

    /** Thread not found for the given UUID. Args: threadId. */
    THREAD_NOT_FOUND("COLLAB-CHT-001"),

    /** Thread could not be lazily created (persistent failure, not a race condition). Args: threadId. */
    THREAD_CREATION_FAILED("COLLAB-CHT-003"),

    /** Message not found for the given UUID. Args: messageId. */
    MESSAGE_NOT_FOUND("COLLAB-CHT-004"),

    // ========== Attachment Module (ATT) ==========

    /** Attachment not found for the given UUID. Args: attachmentId. */
    ATTACHMENT_NOT_FOUND("COLLAB-ATT-001"),

    /** Uploaded file exceeds the maximum allowed size (5 MB). Args: fileName, actualSize, maxSize. */
    FILE_SIZE_EXCEEDED("COLLAB-ATT-002"),

    /** Uploaded file has a blocked extension (e.g., .exe, .bat). Args: fileName, extension. */
    FILE_TYPE_BLOCKED("COLLAB-ATT-003"),

    /** ClamAV detected a virus in the uploaded file. Args: fileName. */
    VIRUS_DETECTED("COLLAB-ATT-004"),

    /** Document conversion to PDF preview failed. Args: reason. */
    DOCUMENT_CONVERSION_FAILED("COLLAB-ATT-005"),

    // ========== Security Module (SEC) ==========

    /** JWT Ticket is invalid, expired, or missing. */
    INVALID_TICKET("COLLAB-SEC-001"),

    /** Ticket does not grant access to the requested thread. Args: threadId. */
    THREAD_ACCESS_DENIED("COLLAB-SEC-002");

    private final String code;

    CollaborationErrorCodes(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }
}