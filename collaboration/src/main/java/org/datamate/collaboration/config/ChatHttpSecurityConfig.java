package org.datamate.collaboration.config;

import lombok.RequiredArgsConstructor;
import org.datamate.collaboration.security.JwtTicketAuthFilter;
import org.datamate.collaboration.security.JwtVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for REST APIs in the Standalone Chat Engine.
 */
// @Configuration
// @EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Allows @PreAuthorize in controllers
@RequiredArgsConstructor
public class ChatHttpSecurityConfig {

    private final JwtVerifier jwtVerifier;
    private final ChatAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable) // Configure properly based on deployment
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/ws/**").permitAll() // WebSockets authenticated separately via interceptors
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/v1/dev/**").permitAll() // Allow generating dev tokens
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtTicketAuthFilter(jwtVerifier), UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
