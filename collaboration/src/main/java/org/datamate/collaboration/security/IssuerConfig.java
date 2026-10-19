package org.datamate.collaboration.security;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration model for an upstream domain service issuer and its RSA public key.
 */
@Getter
@Setter
public class IssuerConfig {

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
     * Optional JWKS URL for dynamic key fetching / rotation.
     */
    private String jwksUrl;
}
