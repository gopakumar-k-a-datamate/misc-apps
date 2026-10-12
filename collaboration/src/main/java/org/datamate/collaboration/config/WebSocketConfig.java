package org.datamate.collaboration.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import org.datamate.collaboration.security.TicketChannelInterceptor;
import org.datamate.collaboration.security.TicketHandshakeInterceptor;

/**
 * WebSocket & STOMP Infrastructure Configuration.
 * Enables real-time pub/sub messaging stream for chat threads.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final TicketHandshakeInterceptor handshakeInterceptor;
    private final TicketChannelInterceptor channelInterceptor;

    public WebSocketConfig(TicketHandshakeInterceptor handshakeInterceptor,
                           TicketChannelInterceptor channelInterceptor) {
        this.handshakeInterceptor = handshakeInterceptor;
        this.channelInterceptor = channelInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Primary STOMP endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(handshakeInterceptor)
                .withSockJS();

        // Plain WebSocket endpoint for clients not using SockJS
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(handshakeInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory message broker routing for topics
        registry.enableSimpleBroker("/topic");

        // Application destination prefix for client-to-server messages
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(channelInterceptor);
    }
}
