package org.datamate.collaboration.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Intercepts incoming STOMP messages.
 * Verifies that the user is authorized to SUBSCRIBE or SEND to the specified destination,
 * based on the threadId extracted during the initial handshake.
 */
@Component
public class TicketChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) ||
                StompCommand.SEND.equals(accessor.getCommand()))) {

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes == null) {
                throw new org.springframework.messaging.MessagingException("No session attributes found");
            }

            String tokenThreadId = (String) sessionAttributes.get("threadId");
            String destination = accessor.getDestination();

            if (tokenThreadId == null) {
                throw new org.springframework.messaging.MessagingException("Unauthorized: Missing thread context in session");
            }

            // Destinations are typically like /topic/{threadId} or /app/messages/{threadId}
            if (destination != null && !destination.contains(tokenThreadId)) {
                throw new org.springframework.messaging.MessagingException("Unauthorized: Cannot access this thread destination");
            }
        }

        return message;
    }
}
