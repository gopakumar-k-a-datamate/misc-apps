package org.datamate.collaboration.security;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for Multi-Domain JWT Ticket Authentication.
 * <p>
 * Implements Section 4 (Multi-Domain Key Management - Option A: Static Configuration)
 * of the Standalone Chat Engine specification.
 * <p>
 * Allows upstream domain services (e.g. restaurant-service, emr-service, finance-service)
 * to register their RSA public keys statically via application configuration.
 */
@Component
@ConfigurationProperties(prefix = "chat.ticket")
@Getter
public class ChatTicketProperties {

    /**
     * List of registered upstream domain service issuers and their corresponding public keys.
     */
    private final List<IssuerConfig> issuers = new ArrayList<>();
}
