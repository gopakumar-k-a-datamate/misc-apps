package org.datamate.collaboration.security;

import java.security.PublicKey;
import java.util.Optional;

/**
 * Strategy interface for resolving public keys used to verify JWT signatures.
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
}
