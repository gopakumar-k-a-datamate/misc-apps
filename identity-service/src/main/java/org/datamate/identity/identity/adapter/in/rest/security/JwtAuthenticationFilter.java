package org.datamate.identity.identity.adapter.in.rest.security;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.datamate.identity.identity.application.port.out.TokenGeneratorPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @EnableLogger
    private Logger log;

    private final SecretKey key;
    private final TokenGeneratorPort tokenGeneratorPort;

    public JwtAuthenticationFilter(
            @Value("${app.security.jwt.secret}") String secret,
            TokenGeneratorPort tokenGeneratorPort) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.tokenGeneratorPort = tokenGeneratorPort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        log.info("JWT filter processing request for URI '{}'", request.getRequestURI());
        final String jwt = authHeader.substring(7);
        if (tokenGeneratorPort != null && tokenGeneratorPort.isBlacklisted(jwt)) {
            log.warn("Attempt to use blacklisted token for URI '{}'", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();

            String userId = claims.getSubject();
            log.info("JWT parsed claims successfully: sub='{}', roles='{}'", userId, claims.get("roles"));

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                List<?> rawRoles = claims.get("roles", List.class);
                if (rawRoles == null) {
                    rawRoles = claims.get("role", List.class);
                }
                List<GrantedAuthority> authorities = new java.util.ArrayList<>();
                if (rawRoles != null) {
                    for (Object roleObj : rawRoles) {
                        String roleName = roleObj.toString();
                        authorities.add(new SimpleGrantedAuthority(roleName));
                        if (!roleName.startsWith("ROLE_")) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
                        }
                    }
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId, null, authorities
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.info("Successfully set SecurityContextHolder for user '{}' with authorities: {}", userId, authorities);
            }
        } catch (Exception e) {
            log.error("JWT token validation failed for URI '{}': {}", request.getRequestURI(), e.getMessage(), e);
        }
        
        filterChain.doFilter(request, response);
    }
}
