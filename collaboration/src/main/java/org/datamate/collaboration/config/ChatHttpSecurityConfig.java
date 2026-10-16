package org.datamate.collaboration.config;

import lombok.RequiredArgsConstructor;
import org.datamate.collaboration.security.JwtTicketAuthFilter;
import org.datamate.collaboration.security.JwtVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for REST APIs in the Standalone Chat Engine.
 * Enforces stateless JWT ticket-based authentication.
 * Adheres strictly to Section 13 of the Chat Engine authentication specification.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class ChatHttpSecurityConfig {

    private final JwtVerifier jwtVerifier;
    private final ChatAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public JwtTicketAuthFilter jwtTicketAuthFilter() {
        return new JwtTicketAuthFilter(jwtVerifier);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtTicketAuthFilter jwtTicketAuthFilter) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/ws", "/ws/**", "/ws-sockjs", "/ws-sockjs/**").permitAll() // WebSockets authenticated separately via interceptors
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtTicketAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
