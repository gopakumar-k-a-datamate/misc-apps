package org.datamate.collaboration.chat.application.dto;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Inbound Command for sending a new message within a conversation thread.
 * Strictly adheres to CQRS naming conventions (Command for state-changing write).
 * Supports text and/or pre-uploaded attachment URLs (RMS flow).
 */
public record SendMessageCommand(
        @Size(max = 1000, message = "{collaboration.chat.message.size.max:Message text must not exceed 1000 characters}")
        String text,

        List<String> attachmentUrls,

        UUID attachmentId,
        UUID parentId) {
    public SendMessageCommand(String text) {
        this(text, null, null, null);
    }

    public SendMessageCommand(String text, UUID attachmentId) {
        this(text, null, attachmentId, null);
    }

    public SendMessageCommand(String text, List<String> attachmentUrls) {
        this(text, attachmentUrls, null, null);
    }
}
