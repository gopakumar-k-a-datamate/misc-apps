package org.datamate.collaboration.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Intercepts incoming STOMP messages across Layers 2, 3, and 4 of WebSocket Security:
 * - Layer 2 (CONNECT): Authenticates client using ticket header if not already done during handshake.
 * - Layer 3 (SUBSCRIBE): Ensures subscription topic matches the ticket's threadId.
 * - Layer 4 (SEND): Ensures destination matches threadId AND verifies WRITE permission.
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

        // Layer 2: STOMP CONNECT authentication
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null && sessionAttributes.get("threadId") != null) {
                String userId = (String) sessionAttributes.get("userId");
                String threadId = (String) sessionAttributes.get("threadId");
                List<String> permissions = getPermissionsFromAttributes(sessionAttributes);
                if (accessor.isMutable()) {
                    accessor.setUser(new TicketPrincipal(userId, threadId, permissions));
                }
                return message; // Already authenticated at HTTP handshake
            }

            // Extract ticket from STOMP CONNECT headers (token, ticket, or Authorization)
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
                    List<String> permissions = extractPermissions(claims);

                    if (sessionAttributes != null) {
                        sessionAttributes.put("userId", userId);
                        sessionAttributes.put("threadId", threadId);
                        sessionAttributes.put("permissions", permissions);
                    }
                    if (accessor.isMutable()) {
                        accessor.setUser(new TicketPrincipal(userId, threadId, permissions));
                    }
                } catch (JwtException e) {
                    throw new MessagingException("Invalid authentication ticket: " + e.getMessage());
                }
            }
            return message;
        }

        // Layer 3: STOMP SUBSCRIBE authorization (thread containment)
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            TicketPrincipal principal = resolvePrincipal(accessor);
            if (principal == null || principal.getThreadId() == null) {
                throw new MessagingException("Unauthorized: Missing thread context in session");
            }

            String destination = accessor.getDestination();
            if (destination != null && !destination.contains(principal.getThreadId())) {
                throw new MessagingException("Unauthorized: Cannot access this thread destination: " + destination);
            }
            return message;
        }

        // Layer 4: STOMP SEND authorization (thread containment + WRITE permission enforcement)
        if (StompCommand.SEND.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();

            TicketPrincipal principal = resolvePrincipal(accessor);
            if (principal == null || principal.getThreadId() == null) {
                throw new MessagingException("Unauthorized: Missing thread context in session");
            }

            if (destination != null && !destination.contains(principal.getThreadId())) {
                throw new MessagingException("Unauthorized: Cannot access this thread destination: " + destination);
            }

            // Enforce WRITE permission
            if (!principal.canWrite()) {
                throw new MessagingException("Unauthorized: Ticket does not have WRITE permission to send messages");
            }
            return message;
        }

        return message;
    }

    private TicketPrincipal resolvePrincipal(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof TicketPrincipal tp) {
            return tp;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.get("threadId") != null) {
            String userId = (String) sessionAttributes.get("userId");
            String threadId = (String) sessionAttributes.get("threadId");
            List<String> permissions = getPermissionsFromAttributes(sessionAttributes);
            TicketPrincipal tp = new TicketPrincipal(userId, threadId, permissions);
            if (accessor.isMutable()) {
                accessor.setUser(tp);
            }
            return tp;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> getPermissionsFromAttributes(Map<String, Object> attributes) {
        Object perms = attributes.get("permissions");
        if (perms instanceof List<?> list) {
            return (List<String>) list;
        }
        return Collections.emptyList();
    }

    private static List<String> extractPermissions(Claims claims) {
        Object permissionsObj = claims.get("permissions");
        if (!(permissionsObj instanceof List<?> permissions)) {
            return Collections.emptyList();
        }
        return permissions.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }
}
