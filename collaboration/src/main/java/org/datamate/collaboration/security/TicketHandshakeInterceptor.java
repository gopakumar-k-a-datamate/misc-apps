package org.datamate.collaboration.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Intercepts the initial WebSocket HTTP upgrade request (Layer 1 of WebSocket auth).
 * Extracts the JWT token from query parameters ("token" or "ticket") or Authorization header,
 * validates it if present, and stores { userId, threadId, permissions } into the WebSocket session attributes.
 * If no token is passed in handshake, allows upgrade so client can authenticate in the STOMP CONNECT frame (Layer 2).
 */
@Component
@RequiredArgsConstructor
public class TicketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtVerifier jwtVerifier;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
            String token = httpServletRequest.getParameter("token");
            if (token == null || token.isBlank()) {
                token = httpServletRequest.getParameter("ticket");
            }
            if (token == null || token.isBlank()) {
                String authHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }

            // If a token is provided on handshake, validate it immediately
            if (token != null && !token.isBlank()) {
                try {
                    Claims claims = jwtVerifier.verify(token);
                    String userId = claims.getSubject();
                    String threadId = claims.get("threadId", String.class);
                    List<String> permissions = extractPermissions(claims);

                    // Store claims in WebSocket session attributes
                    attributes.put("userId", userId);
                    attributes.put("threadId", threadId);
                    attributes.put("permissions", permissions);
                    return true;
                } catch (JwtException e) {
                    // Deny upgrade if token is invalid with explicit 401 Unauthorized status
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                }
            }

            // If no token was provided during HTTP upgrade, allow the WebSocket handshake to complete (101)
            // Authentication will be strictly enforced at the STOMP protocol level (CONNECT frame) by TicketChannelInterceptor
            return true;
        }
        return true;
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

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No action needed after handshake
    }
}
