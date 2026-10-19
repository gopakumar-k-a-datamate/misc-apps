package org.datamate.collaboration.chat.adapter.out.websocket;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import lombok.RequiredArgsConstructor;
import org.datamate.collaboration.chat.application.dto.MessageDto;
import org.datamate.collaboration.chat.application.port.out.MessageBroadcastPort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Outgoing Adapter implementing {@link MessageBroadcastPort} using Spring STOMP / WebSocket.
 * Broadcasts newly persisted chat messages to thread subscribers.
 */
@Component
@RequiredArgsConstructor
public class WebSocketMessageBroadcastAdapter implements MessageBroadcastPort {

    private static final String TOPIC_PREFIX = "/topic/thread.";

    private final SimpMessagingTemplate messagingTemplate;

    @EnableLogger
    private Logger logger;

    @Override
    public void broadcastMessage(UUID threadId, MessageDto message) {
        String destination = TOPIC_PREFIX + threadId;
        if (logger != null) {
            logger.info("Broadcasting message [{}] to destination [{}]", message.id(), destination);
        }
        messagingTemplate.convertAndSend(destination, message);
    }
}
