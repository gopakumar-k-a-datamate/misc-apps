package org.datamate.collaboration.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
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
 * Verifies that the user is authenticated (via Handshake session attributes or CONNECT frame)
 * and authorized to SUBSCRIBE or SEND to the specified destination.
 */
@Component
@RequiredArgsConstructor
public class TicketChannelInterceptor implements ChannelInterceptor {

    private final JwtVerifier jwtVerifier;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        // Authenticate client on initial STOMP CONNECT if not already authenticated during handshake
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null && sessionAttributes.get("threadId") != null) {
                return message; // Already authenticated at HTTP handshake
            }

            // Extract from STOMP CONNECT headers (token, ticket, or Authorization)
            String token = accessor.getFirstNativeHeader("token");
            if (token == null || token.isBlank()) {
                token = accessor.getFirstNativeHeader("ticket");
            }
            if (token == null || token.isBlank()) {
                String authHeader = accessor.getFirstNativeHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }

            if (token != null && !token.isBlank()) {
                try {
                    Claims claims = jwtVerifier.verify(token);
                    String userId = claims.getSubject();
                    String threadId = claims.get("threadId", String.class);
                    if (sessionAttributes != null) {
                        sessionAttributes.put("userId", userId);
                        sessionAttributes.put("threadId", threadId);
                    }
                } catch (JwtException e) {
                    throw new org.springframework.messaging.MessagingException("Invalid authentication ticket: " + e.getMessage());
                }
            }
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) ||
                StompCommand.SEND.equals(accessor.getCommand())) {

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            String tokenThreadId = sessionAttributes != null ? (String) sessionAttributes.get("threadId") : null;

            if (tokenThreadId == null && accessor.getUser() instanceof TicketPrincipal tp) {
                tokenThreadId = tp.getThreadId();
            }

            if (tokenThreadId == null) {
                throw new org.springframework.messaging.MessagingException("Unauthorized: Missing thread context in session");
            }

            // Destinations are typically like /topic/{threadId} or /app/messages/{threadId}
            String destination = accessor.getDestination();
            if (destination != null && !destination.contains(tokenThreadId)) {
                throw new org.springframework.messaging.MessagingException("Unauthorized: Cannot access this thread destination");
            }
        }

        return message;
    }
}
