package org.datamate.collaboration.security;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Development token generation service.
 * Manages in-memory dev RSA keypair and mints test JWT tickets strictly matching the spec:
 * - Header: { alg: "RS256", typ: "JWT", kid: "dev-test-key-1" }
 * - Payload: { iss, sub, threadId, permissions, iat, exp, jti }
 * Active strictly on development and test profiles.
 */
@Service
@Profile({"dev", "local", "test", "default"})
@RequiredArgsConstructor
public class DevTokenService {

    @EnableLogger
    private Logger logger;

    private final KeyRegistry keyRegistry;
    private KeyPair keyPair;
    public static final String DEV_KID = "dev-test-key-1";
    public static final String DEV_ISSUER = "dev-domain-service";

    @PostConstruct
    public void initDevKeys() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        this.keyPair = generator.generateKeyPair();

        // Register public key via the KeyRegistry abstraction (DIP) with issuer metadata
        keyRegistry.registerKey(DEV_KID, DEV_ISSUER, keyPair.getPublic());

        if (logger != null) {
            logger.info("Initialized local development RSA keypair for JWT tickets with kid: {}", DEV_KID);
        }
    }

    /**
     * Mints a valid JWT ticket for a given user and thread with default READ and WRITE permissions.
     *
     * @param userId The user identifier (subject).
     * @param threadId The thread identifier.
     * @return Compact serialized JWT token string.
     */
    public String generateToken(String userId, String threadId) {
        return generateToken(userId, threadId, List.of("READ", "WRITE"));
    }

    /**
     * Mints a valid JWT ticket with custom permissions.
     *
     * @param userId The user identifier (subject).
     * @param threadId The thread identifier.
     * @param permissions The list of granted permissions (e.g. READ, WRITE).
     * @return Compact serialized JWT token string.
     */
    public String generateToken(String userId, String threadId, List<String> permissions) {
        Instant now = Instant.now();
        Instant expiry = now.plus(1, ChronoUnit.HOURS);

        return Jwts.builder()
                .header()
                    .keyId(DEV_KID)
                    .type("JWT")
                .and()
                .issuer(DEV_ISSUER)
                .subject(userId)
                .claim("threadId", threadId)
                .claim("permissions", permissions)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }
}
