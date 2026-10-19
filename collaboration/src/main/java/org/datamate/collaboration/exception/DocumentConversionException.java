package org.datamate.collaboration.exception;

/**
 * Thrown when an error occurs during document-to-PDF conversion in an outbound adapter.
 * <p>
 * Represents an infrastructure/adapter conversion failure rather than a domain validation
 * or client invariant error.
 */
public class DocumentConversionException extends CollaborationBaseException {

    public DocumentConversionException(String message) {
        super(CollaborationErrorCodes.DOCUMENT_CONVERSION_FAILED.code(), message, new Object[]{message});
    }

    public DocumentConversionException(String message, Throwable cause) {
        super(CollaborationErrorCodes.DOCUMENT_CONVERSION_FAILED.code(), message, new Object[]{message}, null, cause);
    }
}