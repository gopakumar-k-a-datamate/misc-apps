package org.datamate.collaboration.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class WebSocketSecurityTest {

    private JwtVerifier jwtVerifier;
    private TicketHandshakeInterceptor handshakeInterceptor;
    private TicketChannelInterceptor channelInterceptor;
    private WebSocketHandler wsHandler;
    private MessageChannel messageChannel;

    @BeforeEach
    void setUp() {
        jwtVerifier = Mockito.mock(JwtVerifier.class);
        handshakeInterceptor = new TicketHandshakeInterceptor(jwtVerifier);
        channelInterceptor = new TicketChannelInterceptor(jwtVerifier);
        wsHandler = Mockito.mock(WebSocketHandler.class);
        messageChannel = Mockito.mock(MessageChannel.class);
    }

    @Test
    @DisplayName("Handshake without token query param should succeed (101) so STOMP CONNECT can authenticate")
    void testHandshakeWithoutToken_ShouldSucceed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Map<String, Object> attributes = new HashMap<>();

        boolean result = handshakeInterceptor.beforeHandshake(
                new ServletServerHttpRequest(request),
                Mockito.mock(ServerHttpResponse.class),
                wsHandler,
                attributes
        );

        assertThat(result).isTrue();
        assertThat(attributes).isEmpty();
    }

    @Test
    @DisplayName("Handshake with valid 'token' parameter should authenticate and store session attributes")
    void testHandshakeWithValidToken_ShouldStoreAttributes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("token", "valid-jwt");
        ServerHttpResponse response = Mockito.mock(ServerHttpResponse.class);
        Map<String, Object> attributes = new HashMap<>();

        Claims claims = Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-42");
        when(claims.get(eq("threadId"), eq(String.class))).thenReturn("thread-abc");
        when(jwtVerifier.verify("valid-jwt")).thenReturn(claims);

        boolean result = handshakeInterceptor.beforeHandshake(
                new ServletServerHttpRequest(request),
                response,
                wsHandler,
                attributes
        );

        assertThat(result).isTrue();
        assertThat(attributes.get("userId")).isEqualTo("user-42");
        assertThat(attributes.get("threadId")).isEqualTo("thread-abc");
    }

    @Test
    @DisplayName("Handshake with valid 'ticket' parameter should authenticate and store session attributes")
    void testHandshakeWithValidTicket_ShouldStoreAttributes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("ticket", "ticket-jwt");
        ServerHttpResponse response = Mockito.mock(ServerHttpResponse.class);
        Map<String, Object> attributes = new HashMap<>();

        Claims claims = Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-99");
        when(claims.get(eq("threadId"), eq(String.class))).thenReturn("thread-xyz");
        when(jwtVerifier.verify("ticket-jwt")).thenReturn(claims);

        boolean result = handshakeInterceptor.beforeHandshake(
                new ServletServerHttpRequest(request),
                response,
                wsHandler,
                attributes
        );

        assertThat(result).isTrue();
        assertThat(attributes.get("userId")).isEqualTo("user-99");
        assertThat(attributes.get("threadId")).isEqualTo("thread-xyz");
    }

    @Test
    @DisplayName("Handshake with invalid token should set 401 UNAUTHORIZED and return false")
    void testHandshakeWithInvalidToken_ShouldReturn401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("token", "bad-jwt");
        ServerHttpResponse response = Mockito.mock(ServerHttpResponse.class);
        Map<String, Object> attributes = new HashMap<>();

        when(jwtVerifier.verify("bad-jwt")).thenThrow(new JwtException("Invalid signature"));

        boolean result = handshakeInterceptor.beforeHandshake(
                new ServletServerHttpRequest(request),
                response,
                wsHandler,
                attributes
        );

        assertThat(result).isFalse();
        Mockito.verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("STOMP CONNECT with header ticket should authenticate session")
    void testStompConnect_WithHeaderTicket_ShouldAuthenticate() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("ticket", "valid-ticket");
        Map<String, Object> sessionAttributes = new HashMap<>();
        accessor.setSessionAttributes(sessionAttributes);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Claims claims = Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn("doctor-1");
        when(claims.get(eq("threadId"), eq(String.class))).thenReturn("thread-med");
        when(jwtVerifier.verify("valid-ticket")).thenReturn(claims);

        Message<?> result = channelInterceptor.preSend(message, messageChannel);

        assertThat(result).isNotNull();
        assertThat(sessionAttributes.get("userId")).isEqualTo("doctor-1");
        assertThat(sessionAttributes.get("threadId")).isEqualTo("thread-med");
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE to matching thread topic should succeed")
    void testStompSubscribe_MatchingThread_ShouldSucceed() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/thread-123");
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("threadId", "thread-123");
        accessor.setSessionAttributes(sessionAttributes);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = channelInterceptor.preSend(message, messageChannel);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE to mismatched thread topic should be rejected")
    void testStompSubscribe_MismatchedThread_ShouldThrowMessagingException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/other-thread-999");
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("threadId", "thread-123");
        accessor.setSessionAttributes(sessionAttributes);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> channelInterceptor.preSend(message, messageChannel))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("Cannot access this thread destination");
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE without thread context should be rejected")
    void testStompSubscribe_NoThreadContext_ShouldThrowMessagingException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/thread-123");
        accessor.setSessionAttributes(new HashMap<>());
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> channelInterceptor.preSend(message, messageChannel))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("Missing thread context");
    }

    @Test
    @DisplayName("STOMP SEND with WRITE permission should succeed")
    void testStompSend_WithWritePermission_ShouldSucceed() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/thread-123.sendMessage");
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("threadId", "thread-123");
        sessionAttributes.put("userId", "john");
        sessionAttributes.put("permissions", java.util.List.of("READ", "WRITE"));
        accessor.setSessionAttributes(sessionAttributes);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = channelInterceptor.preSend(message, messageChannel);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("STOMP SEND with READ-only permission should be rejected")
    void testStompSend_WithReadOnlyPermission_ShouldThrowMessagingException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/thread-123.sendMessage");
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("threadId", "thread-123");
        sessionAttributes.put("userId", "reader");
        sessionAttributes.put("permissions", java.util.List.of("READ"));
        accessor.setSessionAttributes(sessionAttributes);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> channelInterceptor.preSend(message, messageChannel))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("WRITE permission");
    }
}
