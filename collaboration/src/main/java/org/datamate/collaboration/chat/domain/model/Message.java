package org.datamate.collaboration.chat.domain.model;

import lombok.Getter;
import org.datamate.collaboration.exception.CollaborationErrorCodes;
import org.datamate.collaboration.exception.DomainValidationException;

import java.time.Instant;

import java.util.UUID;

/**
 * Message Domain Entity.
 * <p>
 * Represents a single message within a {@link Thread}. Uses a raw {@code threadId}
 * (UUID foreign key) instead of a JPA entity reference to avoid N+1 queries and
 * keep aggregate boundaries clean.
 * <p>
 * Supports a 1-to-1 attachment model: {@code attachmentId} optionally links to an
 * {@link Attachment}, and {@code text} serves as the caption when {@code isFile} is true.
 * <p>
 * Pure Domain Entity — no Spring, JPA, or framework annotations.
 */
@Getter
public class Message {

    private final UUID id;
    private final UUID threadId;
    private final String senderId;
    private final String text;
    private final boolean file;
    private final boolean systemMessage;
    private final UUID attachmentId;
    private final UUID parentId;
    private final Instant timestamp;

    private Message(UUID id, UUID threadId, String senderId, String text, boolean file, boolean systemMessage, UUID attachmentId, UUID parentId, Instant timestamp) {
        requireNonNull(id, "messageId");
        requireNonNull(threadId, "threadId");
        requireNonNull(senderId, "senderId");
        requireNonNull(timestamp, "timestamp");

        this.id = id;
        this.threadId = threadId;
        this.senderId = senderId;
        this.text = text;
        this.file = file;
        this.systemMessage = systemMessage;
        this.attachmentId = attachmentId;
        this.parentId = parentId;
        this.timestamp = timestamp;
    }

    /**
     * Factory method for creating a brand new Message.
     * Encapsulates ID generation and timestamping within the domain.
     */
    public static Message create(UUID threadId, String senderId, String text, boolean file, boolean systemMessage, UUID attachmentId, UUID parentId) {
        return new Message(
                UuidV7Generator.generate(),
                threadId,
                senderId,
                text,
                file,
                systemMessage,
                attachmentId,
                parentId, Instant.now()
        );
    }

    /**
     * Factory method for reconstituting an existing Message from persistence.
     */
    public static Message restore(UUID id, UUID threadId, String senderId, String text, boolean file, boolean systemMessage, UUID attachmentId, UUID parentId, Instant timestamp) {
        return new Message(id, threadId, senderId, text, file, systemMessage, attachmentId, parentId, timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message that)) return false;
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


