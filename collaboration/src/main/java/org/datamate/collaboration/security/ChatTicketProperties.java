package org.datamate.collaboration.security;

import lombok.Getter;
import lombok.Setter;
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
@Setter
public class ChatTicketProperties {

    /**
     * List of registered upstream domain service issuers and their corresponding public keys.
     */
    private List<IssuerConfig> issuers = new ArrayList<>();

    @Getter
    @Setter
    public static class IssuerConfig {
        /**
         * The issuer identifier (must match 'iss' claim in JWT tickets).
         * Example: "restaurant-service", "emr-service", "finance-service"
         */
        private String issuer;

        /**
         * Key ID for this public key (must match 'kid' header in JWT tickets).
         * Example: "restaurant-key-v1"
         */
        private String kid;

        /**
         * RSA Public Key in PEM format (or Base64-encoded X.509 format).
         */
        private String publicKey;

        /**
         * Optional JWKS URL for dynamic key fetching / rotation (Option B).
         */
        private String jwksUrl;
    }
}
