package org.datamate.collaboration.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Intercepts the initial WebSocket HTTP upgrade request.
 * Extracts the JWT token from the query parameters, validates it,
 * and sets the claims into the WebSocket session attributes.
 */
@Component
@RequiredArgsConstructor
public class TicketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtVerifier jwtVerifier;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");

            if (token == null || token.isBlank()) {
                // Deny upgrade if no token is provided
                return false;
            }

            try {
                Claims claims = jwtVerifier.verify(token);
                // Store claims in WebSocket session attributes
                attributes.put("userId", claims.getSubject());
                attributes.put("threadId", claims.get("threadId", String.class));
                return true;
            } catch (JwtException e) {
                // Deny upgrade if token is invalid
                return false;
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No action needed after handshake
    }
}
