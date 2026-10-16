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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Middleware filter that intercepts incoming REST requests, extracts and verifies the signed JWT Ticket,
 * enforces request-level threadId binding (Section 6 of Chat Engine specification),
 * and sets the authenticated TicketPrincipal in the SecurityContext.
 */
@RequiredArgsConstructor
public class JwtTicketAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Pattern THREAD_PATH_PATTERN = Pattern.compile("/threads/([^/]+)");

    private final JwtVerifier jwtVerifier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @EnableLogger
    private Logger logger;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            token = authHeader.substring(BEARER_PREFIX.length()).trim();
        } else {
            token = request.getParameter("ticket");
            if (token == null || token.isBlank()) {
                token = request.getParameter("token");
            }
            if (token != null) {
                token = token.trim();
            }
        }

        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Validate the signed JWT ticket
            Claims claims = jwtVerifier.verify(token);

            // Step 5: Request-level threadId binding (Section 6 of plan)
            String urlThreadId = extractThreadIdFromPath(request);
            String tokenThreadId = claims.get("threadId", String.class);

            if (urlThreadId != null && tokenThreadId != null && !urlThreadId.equalsIgnoreCase(tokenThreadId)) {
                if (logger != null) {
                    logger.warn("Thread ID binding mismatch: path [{}] does not match token [{}]", urlThreadId, tokenThreadId);
                }
                sendForbidden(response, "Access denied: ticket is not valid for thread " + urlThreadId);
                return;
            }

            // Create Authentication and bind to Security Context
            TicketAuthentication authentication = new TicketAuthentication(claims);

            // Action-level permission check (READ vs WRITE)
            String method = request.getMethod();
            TicketPrincipal ticketPrincipal = authentication.getTicketPrincipal();
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
                if (!ticketPrincipal.getPermissions().isEmpty() && !ticketPrincipal.canWrite()) {
                    if (logger != null) {
                        logger.warn("Permission denied: ticket lacks WRITE permission for method [{}] on path [{}]", method, request.getRequestURI());
                    }
                    sendForbidden(response, "Access denied: ticket does not grant WRITE permission");
                    return;
                }
            } else if ("GET".equalsIgnoreCase(method)) {
                if (!ticketPrincipal.getPermissions().isEmpty() && !ticketPrincipal.canRead()) {
                    if (logger != null) {
                        logger.warn("Permission denied: ticket lacks READ permission for method [{}] on path [{}]", method, request.getRequestURI());
                    }
                    sendForbidden(response, "Access denied: ticket does not grant READ permission");
                    return;
                }
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);

            if (logger != null) {
                logger.debug("Successfully authenticated user: {} for thread: {}",
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

    private String extractThreadIdFromPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return null;
        }
        Matcher matcher = THREAD_PATH_PATTERN.matcher(uri);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

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

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/ws") || path.startsWith("/ws/")
                || path.equals("/ws-sockjs") || path.startsWith("/ws-sockjs/");
    }
}
