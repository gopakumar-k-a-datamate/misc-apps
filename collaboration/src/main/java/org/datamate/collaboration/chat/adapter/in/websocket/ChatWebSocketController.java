package org.datamate.collaboration.chat.adapter.in.websocket;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.datamate.collaboration.chat.application.dto.RefreshTicketCommand;
import org.datamate.collaboration.chat.application.dto.SendMessageCommand;
import org.datamate.collaboration.chat.application.port.in.SendMessageUseCase;
import org.datamate.collaboration.security.JwtVerifier;
import org.datamate.collaboration.security.TicketPrincipal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Incoming WebSocket Adapter (STOMP MessageMapping).
 * <p>
 * Handles inbound STOMP messages across application destinations:
 * <ul>
 *   <li>{@code /app/thread.{threadId}.sendMessage} - Inbound real-time message submission</li>
 *   <li>{@code /app/refresh-ticket} - In-flight STOMP session ticket renewal</li>
 * </ul>
 * Strictly adheres to Hexagonal Architecture (Inbound Adapter) and Sections 5 &amp; 9 of the Chat specification.
 */
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    @EnableLogger
    private Logger logger;

    private final SendMessageUseCase sendMessageUseCase;
    private final JwtVerifier jwtVerifier;

    /**
     * Handles incoming chat messages sent directly over WebSocket STOMP.
     * Destination: {@code /app/thread.{threadId}.sendMessage}
     *
     * @param threadId The target thread identifier.
     * @param command The message send command payload.
     * @param principal The authenticated security principal.
     */
    @MessageMapping("/thread.{threadId}.sendMessage")
    public void handleSendMessage(
            @DestinationVariable UUID threadId,
            @Valid @Payload SendMessageCommand command,
            Principal principal) {

        String senderId = (principal != null && principal.getName() != null) ? principal.getName() : "Anonymous";

        if (logger != null) {
            logger.info("Received WebSocket STOMP message for thread [{}] from sender [{}]", threadId, senderId);
        }

        sendMessageUseCase.sendMessage(threadId, senderId, command);
    }

    /**
     * Handles in-flight ticket refresh for persistent WebSocket connections.
     * Destination: {@code /app/refresh-ticket}
     * Adheres to Section 9 (Ticket TTL &amp; Refresh Strategy).
     *
     * @param command The ticket refresh command containing the newly minted ticket.
     * @param headerAccessor The STOMP message header accessor to update session attributes.
     * @param principal The current authenticated principal.
     * @return Confirmation status map.
     */
    @MessageMapping("/refresh-ticket")
    @SendToUser("/topic/refresh-status")
    public Map<String, Object> handleRefreshTicket(
            @Valid @Payload RefreshTicketCommand command,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {

        if (command == null || command.ticket() == null || command.ticket().isBlank()) {
            throw new IllegalArgumentException("Refresh ticket token must not be blank");
        }

        // Verify the newly supplied ticket
        Claims claims = jwtVerifier.verify(command.ticket());
        String newUserId = claims.getSubject();
        String newThreadId = claims.get("threadId", String.class);
        List<String> newPermissions = extractPermissions(claims);

        // Security binding: verify refreshed ticket matches current session subject & threadId
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            String currentThreadId = (String) sessionAttributes.get("threadId");
            String currentUserId = (String) sessionAttributes.get("userId");

            if (currentThreadId != null && !currentThreadId.equalsIgnoreCase(newThreadId)) {
                throw new SecurityException("Refreshed ticket threadId [" + newThreadId + "] does not match active session threadId [" + currentThreadId + "]");
            }
            if (currentUserId != null && !currentUserId.equalsIgnoreCase(newUserId)) {
                throw new SecurityException("Refreshed ticket userId [" + newUserId + "] does not match active session userId [" + currentUserId + "]");
            }

            // Update session attributes
            sessionAttributes.put("userId", newUserId);
            sessionAttributes.put("threadId", newThreadId);
            sessionAttributes.put("permissions", newPermissions);
        }

        if (headerAccessor.isMutable()) {
            headerAccessor.setUser(new TicketPrincipal(newUserId, newThreadId, newPermissions));
        }

        if (logger != null) {
            logger.info("Successfully refreshed STOMP session ticket for user [{}] on thread [{}]", newUserId, newThreadId);
        }

        return Map.of(
                "status", "SUCCESS",
                "threadId", newThreadId,
                "userId", newUserId,
                "message", "WebSocket session ticket refreshed successfully"
        );
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
