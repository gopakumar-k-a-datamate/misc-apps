package org.datamate.collaboration.chat.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * CQRS Command object for refreshing a client's WebSocket session ticket.
 * Adheres to Section 9 (Ticket TTL & Refresh Strategy) of the Standalone Chat Engine specification.
 *
 * @param ticket the newly issued JWT Chat Ticket minted by the upstream Domain Service.
 */
public record RefreshTicketCommand(
        @NotBlank(message = "Ticket token must not be blank")
        String ticket
) {
}
