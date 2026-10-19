package org.datamate.identity.shared.config;

import lombok.RequiredArgsConstructor;
import org.datamate.identity.identity.adapter.in.rest.security.JwtAuthenticationFilter;
import org.datamate.identity.shared.config.security.RevisionMetadataFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

import com.datamate.bedrock.framework.common.security.config.SecurityProperties;
import com.datamate.bedrock.framework.common.security.jwt.service.JwtTokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
@Import(JwtTokenService.class)
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RevisionMetadataFilter revisionMetadataFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                .requestMatchers("/actuator/**", "/error").permitAll()
                // OPA sidecar bundle polling and Admin UI policy management
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/internal/authz/bundle/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/internal/authz/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/internal/authz/policies").permitAll()
                // Subject discovery endpoints for Admin UI
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/roles", "/api/v1/roles/select", "/api/v1/users").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/users/*/change-password").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                // All other endpoints require authentication and are enforced dynamically by OPA PEP
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(revisionMetadataFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*")); // Admin UI and Flutter app
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;

    }
}



