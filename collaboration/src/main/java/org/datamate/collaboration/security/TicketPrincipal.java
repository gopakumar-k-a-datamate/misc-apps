package org.datamate.collaboration.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

/**
 * Custom Principal for Delegated Chat Tickets.
 * Encapsulates the user's identity and the bound chat thread context.
 */
@Getter
@AllArgsConstructor
public class TicketPrincipal implements Principal {

    private final String userId;
    private final String threadId;

    @Override
    public String getName() {
        return this.userId;
    }
}
