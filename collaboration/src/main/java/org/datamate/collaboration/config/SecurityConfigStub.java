package org.datamate.collaboration.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(SecurityFilterChain.class)
public class SecurityConfigStub {

    private final AuthenticationFilter authenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(org.springframework.security.config.Customizer.withDefaults()).csrf(AbstractHttpConfigurer::disable)
            // TODO (Epic 3): Replace StubAuthenticationFilter with JwtAuthenticationFilter
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/ws/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            // Inject our stub filter before standard auth
            .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }
}


