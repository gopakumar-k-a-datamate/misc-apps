package org.datamate.collaboration.chat.application.dto;

import java.util.UUID;

/**
 * CQRS Command representing an incoming background system status event or automated audit action
 * to be recorded as an immutable, read-only system message in a chat thread.
 *
 * @param threadId UUID of the chat thread where the event should be posted (required)
 * @param senderId Identifier of the automated system or service producing the event (defaults to "SYSTEM")
 * @param text The descriptive status or audit log text (required)
 * @param parentId Optional parent message UUID for threaded replies / status correlation
 */
public record RecordSystemEventCommand(
        UUID threadId,
        String senderId,
        String text,
        UUID parentId
) {
    public RecordSystemEventCommand {
        if (senderId == null || senderId.isBlank()) {
            senderId = "SYSTEM";
        }
    }

    public static RecordSystemEventCommand of(UUID threadId, String text) {
        return new RecordSystemEventCommand(threadId, "SYSTEM", text, null);
    }

    public static RecordSystemEventCommand of(UUID threadId, String senderId, String text) {
        return new RecordSystemEventCommand(threadId, senderId, text, null);
    }
}
