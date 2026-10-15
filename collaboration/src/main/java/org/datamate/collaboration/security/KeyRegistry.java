package org.datamate.collaboration.security;

import java.security.PublicKey;
import java.util.Optional;

/**
 * Strategy interface for resolving public keys used to verify JWT signatures.
 * Follows the Dependency Inversion Principle (DIP) to decouple authentication
 * from specific key storage or dynamic JWKS retrieval mechanisms.
 */
public interface KeyRegistry {
    
    /**
     * Retrieves the public key associated with the given Key ID (kid).
     * 
     * @param kid The key identifier.
     * @return An Optional containing the PublicKey if found, empty otherwise.
     */
    Optional<PublicKey> getKey(String kid);

    /**
     * Registers a public key associated with the given Key ID.
     *
     * @param kid The key identifier.
     * @param publicKey The public key.
     */
    void registerKey(String kid, PublicKey publicKey);

    /**
     * Registers a base64/PEM encoded public key associated with the given Key ID.
     *
     * @param kid The key identifier.
     * @param base64PublicKey The base64 or PEM encoded public key.
     */
    void registerKey(String kid, String base64PublicKey);

    /**
     * Registers a public key associated with a specific Key ID and issuer.
     *
     * @param kid The key identifier.
     * @param issuer The domain service issuer name.
     * @param publicKey The public key.
     */
    default void registerKey(String kid, String issuer, PublicKey publicKey) {
        registerKey(kid, publicKey);
    }

    /**
     * Registers a base64/PEM encoded public key associated with a specific Key ID and issuer.
     *
     * @param kid The key identifier.
     * @param issuer The domain service issuer name.
     * @param base64PublicKey The base64 or PEM encoded public key.
     */
    default void registerKey(String kid, String issuer, String base64PublicKey) {
        registerKey(kid, base64PublicKey);
    }

    /**
     * Retrieves the issuer associated with the given Key ID (kid).
     *
     * @param kid The key identifier.
     * @return An Optional containing the issuer string if registered, empty otherwise.
     */
    default Optional<String> getIssuerForKid(String kid) {
        return Optional.empty();
    }

    /**
     * Verifies if the given issuer is known and registered.
     *
     * @param issuer The issuer name from the 'iss' JWT claim.
     * @return true if the issuer is recognized, false otherwise.
     */
    default boolean isKnownIssuer(String issuer) {
        return true;
    }
}
