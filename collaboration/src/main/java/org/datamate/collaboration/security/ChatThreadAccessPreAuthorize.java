package org.datamate.collaboration.security;

import org.datamate.authz.enforcement.PolicyEnforcer;
import org.datamate.collaboration.chat.application.dto.ChatThreadPolicyResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Custom SpEL evaluator for Chat Thread OPA Authorization.
 * Can be used in controllers like: @PreAuthorize("@chatAuthorizer.hasAccess(#threadId, 'WRITE')")
 */
@Component("chatAuthorizer")
public class ChatThreadAccessPreAuthorize {

    private final PolicyEnforcer enforcer;

    public ChatThreadAccessPreAuthorize(PolicyEnforcer enforcer) {
        this.enforcer = enforcer;
    }

    public boolean hasAccess(String requestedThreadId, String action) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof TicketPrincipal)) {
            // Unauthenticated or wrong principal type
            return false;
        }

        TicketPrincipal principal = (TicketPrincipal) authentication.getPrincipal();

        ChatThreadPolicyResource resource = new ChatThreadPolicyResource();
        resource.setRequestedThreadId(requestedThreadId);
        resource.setTokenThreadId(principal.getThreadId());
        resource.setAction(action);

        // This will throw an exception if OPA denies access, which Spring Security converts to 403
        enforcer.enforce(resource);
        return true;
    }
}
