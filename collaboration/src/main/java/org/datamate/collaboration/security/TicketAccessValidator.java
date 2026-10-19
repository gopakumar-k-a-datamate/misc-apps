package org.datamate.collaboration.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Access control and authorization validator for Delegated Chat Tickets.
 * <p>
 * Enforces:
 * 1. Thread Confinement: validates that the requested resource thread matches the ticket's bound threadId.
 * 2. Action Permissions: validates that the HTTP verb corresponds to granted permissions (READ vs WRITE).
 */
@Component
public class TicketAccessValidator {

    private static final Pattern THREAD_PATH_PATTERN = Pattern.compile("/threads/([^/]+)");

    /**
     * Validates that the request has access to the target thread and operation.
     *
     * @param request   the HTTP servlet request
     * @param principal the authenticated ticket principal
     * @return an Optional containing the error detail if forbidden, or empty if permitted
     */
    public Optional<String> validateAccess(HttpServletRequest request, TicketPrincipal principal) {
        if (principal == null) {
            return Optional.of("Access denied: missing principal");
        }

        // Thread Confinement Check
        String urlThreadId = extractThreadId(request.getRequestURI());
        if (urlThreadId != null && !urlThreadId.equalsIgnoreCase(principal.getThreadId())) {
            return Optional.of("Access denied: ticket is not valid for thread " + urlThreadId);
        }

        // Action Permission Check
        if (!hasRequiredPermission(request.getMethod(), principal)) {
            String detail = isModifyingMethod(request.getMethod())
                    ? "Access denied: ticket does not grant WRITE permission"
                    : "Access denied: ticket does not grant READ permission";
            return Optional.of(detail);
        }

        return Optional.empty();
    }

    // =========================================================================
    // Private Implementation Details
    // =========================================================================

    private boolean hasRequiredPermission(String method, TicketPrincipal principal) {
        if (principal.getPermissions().isEmpty()) {
            return true;
        }

        if (isModifyingMethod(method)) {
            return principal.canWrite();
        }
        if (HttpMethod.GET.matches(method)) {
            return principal.canRead();
        }
        return true;
    }

    private boolean isModifyingMethod(String method) {
        return HttpMethod.POST.matches(method)
                || HttpMethod.PUT.matches(method)
                || HttpMethod.DELETE.matches(method)
                || HttpMethod.PATCH.matches(method);
    }

    private String extractThreadId(String uri) {
        if (uri == null) {
            return null;
        }
        Matcher matcher = THREAD_PATH_PATTERN.matcher(uri);
        return matcher.find() ? matcher.group(1) : null;
    }
}
