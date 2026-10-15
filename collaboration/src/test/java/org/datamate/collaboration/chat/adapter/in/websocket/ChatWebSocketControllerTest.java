package org.datamate.collaboration.chat.adapter.in.websocket;

import io.jsonwebtoken.Claims;
import org.datamate.collaboration.chat.application.dto.RefreshTicketCommand;
import org.datamate.collaboration.chat.application.dto.SendMessageCommand;
import org.datamate.collaboration.chat.application.port.in.SendMessageUseCase;
import org.datamate.collaboration.security.JwtVerifier;
import org.datamate.collaboration.security.TicketPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatWebSocketControllerTest {

    private SendMessageUseCase sendMessageUseCase;
    private JwtVerifier jwtVerifier;
    private ChatWebSocketController controller;

    @BeforeEach
    void setUp() {
        sendMessageUseCase = Mockito.mock(SendMessageUseCase.class);
        jwtVerifier = Mockito.mock(JwtVerifier.class);
        controller = new ChatWebSocketController(sendMessageUseCase, jwtVerifier);
    }

    @Test
    @DisplayName("handleSendMessage: should invoke SendMessageUseCase with principal name and command")
    void testHandleSendMessage_Success() {
        UUID threadId = UUID.randomUUID();
        SendMessageCommand command = new SendMessageCommand("Hello via STOMP", null, null, null);
        Principal principal = () -> "doctor-1";

        controller.handleSendMessage(threadId, command, principal);

        verify(sendMessageUseCase).sendMessage(threadId, "doctor-1", command);
    }

    @Test
    @DisplayName("handleRefreshTicket: should validate ticket and update session attributes")
    void testHandleRefreshTicket_Success() {
        String newTicket = "valid-new-ticket";
        RefreshTicketCommand command = new RefreshTicketCommand(newTicket);

        Claims claims = Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn("doctor-1");
        when(claims.get("threadId", String.class)).thenReturn("thread-123");
        when(claims.get("permissions")).thenReturn(List.of("READ", "WRITE"));
        when(jwtVerifier.verify(newTicket)).thenReturn(claims);

        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("userId", "doctor-1");
        sessionAttrs.put("threadId", "thread-123");
        accessor.setSessionAttributes(sessionAttrs);

        Principal principal = new TicketPrincipal("doctor-1", "thread-123", List.of("READ"));

        Map<String, Object> result = controller.handleRefreshTicket(command, accessor, principal);

        assertThat(result.get("status")).isEqualTo("SUCCESS");
        assertThat(sessionAttrs.get("permissions")).isEqualTo(List.of("READ", "WRITE"));
    }

    @Test
    @DisplayName("handleRefreshTicket: mismatched threadId should throw SecurityException")
    void testHandleRefreshTicket_MismatchedThread_ShouldThrow() {
        String newTicket = "different-thread-ticket";
        RefreshTicketCommand command = new RefreshTicketCommand(newTicket);

        Claims claims = Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn("doctor-1");
        when(claims.get("threadId", String.class)).thenReturn("thread-other-999");
        when(jwtVerifier.verify(newTicket)).thenReturn(claims);

        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("userId", "doctor-1");
        sessionAttrs.put("threadId", "thread-123");
        accessor.setSessionAttributes(sessionAttrs);

        Principal principal = new TicketPrincipal("doctor-1", "thread-123");

        assertThatThrownBy(() -> controller.handleRefreshTicket(command, accessor, principal))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("does not match active session threadId");
    }
}
