package org.datamate.collaboration.security;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

/**
 * Middleware filter that intercepts incoming REST requests, extracts and verifies the signed JWT Ticket,
 * delegates authorization to {@link TicketAccessValidator}, and sets the authenticated
 * TicketPrincipal in the SecurityContext.
 * <p>
 * Follows Separation of Concerns (SoC) by delegating business authorization to {@link TicketAccessValidator}
 * and keeping the filter focused purely on HTTP token resolution, authentication, and error formatting.
 */
@Component
public class JwtTicketAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtVerifier jwtVerifier;
    private final TicketAccessValidator accessValidator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @EnableLogger
    private Logger logger;

    public JwtTicketAuthFilter(JwtVerifier jwtVerifier, @Autowired(required = false) TicketAccessValidator accessValidator) {
        this.jwtVerifier = jwtVerifier;
        this.accessValidator = accessValidator != null ? accessValidator : new TicketAccessValidator();
    }

    public JwtTicketAuthFilter(JwtVerifier jwtVerifier) {
        this(jwtVerifier, new TicketAccessValidator());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Token Resolution
        String token = resolveToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2. Authentication
            TicketAuthentication authentication = authenticate(token);
            TicketPrincipal principal = authentication.getTicketPrincipal();

            // 3. Authorization (delegated to TicketAccessValidator)
            if (!isAuthorized(request, principal, response)) {
                return;
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);

            if (logger != null) {
                logger.debug("Successfully authenticated user: [{}] for thread: [{}]",
                        authentication.getSub(), authentication.getThreadId());
            }

        } catch (JwtException e) {
            if (logger != null) {
                logger.warn("JWT Ticket verification failed: {}", e.getMessage());
            }
            SecurityContextHolder.clearContext();
            // Let AuthenticationEntryPoint uniformly handle 401 Unauthorized
        }

        filterChain.doFilter(request, response);
    }

    // =========================================================================
    // 1. Token Resolution
    // =========================================================================

    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }

        String ticket = request.getParameter("ticket");
        if (ticket == null || ticket.isBlank()) {
            ticket = request.getParameter("token");
        }
        return (ticket != null && !ticket.isBlank()) ? ticket.trim() : null;
    }

    // =========================================================================
    // 2. Authentication
    // =========================================================================

    private TicketAuthentication authenticate(String token) {
        Claims claims = jwtVerifier.verify(token);
        return new TicketAuthentication(claims);
    }

    // =========================================================================
    // 3. Authorization (Delegated to TicketAccessValidator)
    // =========================================================================

    private boolean isAuthorized(HttpServletRequest request, TicketPrincipal principal, HttpServletResponse response)
            throws IOException {

        java.util.Optional<String> forbiddenReason = accessValidator.validateAccess(request, principal);
        if (forbiddenReason.isPresent()) {
            if (logger != null) {
                logger.warn("Access denied on [{}] for user [{}]: {}",
                        request.getRequestURI(), principal.getName(), forbiddenReason.get());
            }
            sendForbidden(response, forbiddenReason.get());
            return false;
        }

        return true;
    }

    // =========================================================================
    // 4. Error Response Formatting
    // =========================================================================

    private void sendForbidden(HttpServletResponse response, String detail) throws IOException {
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, detail);
        problemDetail.setTitle("Forbidden");
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setProperty("errorCode", "AUTH_FORBIDDEN");
        problemDetail.setProperty("timestamp", Instant.now().toString());

        response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
    }

    // =========================================================================
    // 5. Endpoint Bypass Routing
    // =========================================================================

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/ws") || path.startsWith("/ws/")
                || path.equals("/ws-sockjs") || path.startsWith("/ws-sockjs/");
    }
}
