package org.datamate.collaboration.chat.domain.model;

import lombok.Getter;
import org.datamate.collaboration.exception.CollaborationErrorCodes;
import org.datamate.collaboration.exception.DomainValidationException;


import java.util.UUID;

/**
 * Attachment Domain Entity.
 * <p>
 * Represents a file uploaded to the chat system. Each Attachment has a 1-to-1
 * relationship with a {@link Message} (linked via {@code attachmentId}).
 * The message's {@code text} field serves as the attachment's caption.
 * <p>
 * {@code previewUrl} is nullable and populated asynchronously when non-PDF
 * documents are converted to preview PDFs (Epic 6 — Parked Feature).
 * <p>
 * Pure Domain Entity — no Spring, JPA, or framework annotations.
 */
@Getter
public class Attachment {

    private final UUID id;
    private final String fileName;
    private final String mimeType;
    private final long fileSize;
    private final String uploadUrl;
    private final String previewUrl;

    private Attachment(UUID id, String fileName, String mimeType, long fileSize, String uploadUrl, String previewUrl) {
        requireNonNull(id, "attachmentId");
        requireNonNull(fileName, "fileName");
        requireNonNull(mimeType, "mimeType");
        requireNonNull(uploadUrl, "uploadUrl");

        this.id = id;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.uploadUrl = uploadUrl;
        this.previewUrl = previewUrl;
    }

    /**
     * Factory method for creating a brand new Attachment.
     * Encapsulates ID generation within the domain.
     */
    public static Attachment create(String fileName, String mimeType, long fileSize, String uploadUrl, String previewUrl) {
        return new Attachment(
                UuidV7Generator.generate(),
                fileName,
                mimeType,
                fileSize,
                uploadUrl,
                previewUrl
        );
    }

    /**
     * Factory method for reconstituting an existing Attachment from persistence.
     */
    public static Attachment restore(UUID id, String fileName, String mimeType, long fileSize, String uploadUrl, String previewUrl) {
        return new Attachment(id, fileName, mimeType, fileSize, uploadUrl, previewUrl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Attachment that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Domain-level guard: throws {@link DomainValidationException} with
     * the reusable {@code REQUIRED_FIELD_MISSING} error code.
     */
    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new DomainValidationException(
                    CollaborationErrorCodes.REQUIRED_FIELD_MISSING.code(), fieldName);
        }
    }
}
