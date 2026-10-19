package org.datamate.collaboration.chat.application.port.out;

import org.datamate.collaboration.chat.application.dto.MessageDto;

import java.util.UUID;

/**
 * Outbound Port for real-time broadcast of chat messages.
 * Implementations fan out new messages to subscribed WebSocket clients.
 */
public interface MessageBroadcastPort {

    /**
     * Broadcasts a persisted message to all subscribers of the given discussion thread.
     *
     * @param threadId the discussion thread identifier
     * @param message  the DTO representation of the persisted message
     */
    void broadcastMessage(UUID threadId, MessageDto message);
}
