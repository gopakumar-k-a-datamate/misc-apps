package org.datamate.collaboration.chat.adapter.in.messaging.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.datamate.collaboration.chat.application.dto.RecordSystemEventCommand;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire DTO representing an incoming system status event payload from the message broker.
 * <p>
 * Part of the Anti-Corruption Layer (ACL) in the Inbound Messaging Adapter.
 * Decouples external broker message structures from the internal {@link RecordSystemEventCommand}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatSystemEventPayload(
        UUID threadId,
        String sourceService,
        String eventType,
        String message,
        String text,
        UUID parentId,
        Instant timestamp
) {

    /**
     * Converts this external payload into the application use case command.
     */
    public RecordSystemEventCommand toCommand() {
        String content = (text != null && !text.isBlank()) ? text : message;
        String sender = (sourceService != null && !sourceService.isBlank()) ? sourceService : "SYSTEM";
        return new RecordSystemEventCommand(threadId, sender, content, parentId);
    }
}
