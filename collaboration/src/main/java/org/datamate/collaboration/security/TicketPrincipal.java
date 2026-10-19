package org.datamate.collaboration.security;

import lombok.Getter;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

/**
 * Custom Principal for Delegated Chat Tickets.
 * Encapsulates the user's identity, the bound chat thread context, and authorized permissions.
 */
@Getter
public class TicketPrincipal implements Principal {

    private final String userId;
    private final String threadId;
    private final List<String> permissions;

    public TicketPrincipal(String userId, String threadId, List<String> permissions) {
        this.userId = userId;
        this.threadId = threadId;
        this.permissions = permissions != null ? List.copyOf(permissions) : Collections.emptyList();
    }

    public TicketPrincipal(String userId, String threadId) {
        this(userId, threadId, Collections.emptyList());
    }

    @Override
    public String getName() {
        return this.userId;
    }

    public boolean hasPermission(String permission) {
        if (permission == null || permissions.isEmpty()) {
            return false;
        }
        return permissions.stream().anyMatch(p -> p.equalsIgnoreCase(permission));
    }

    public boolean canRead() {
        return hasPermission("READ");
    }

    public boolean canWrite() {
        return hasPermission("WRITE");
    }
}
