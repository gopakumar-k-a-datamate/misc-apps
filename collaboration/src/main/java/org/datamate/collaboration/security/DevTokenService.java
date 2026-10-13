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

/**
 * Development token generation service.
 * Manages in-memory dev RSA keypair and mints test JWT tickets.
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

    @PostConstruct
    public void initDevKeys() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        this.keyPair = generator.generateKeyPair();

        // Register public key via the KeyRegistry abstraction (DIP)
        keyRegistry.registerKey(DEV_KID, keyPair.getPublic());

        if (logger != null) {
            logger.info("Initialized local development RSA keypair for JWT tickets with kid: {}", DEV_KID);
        }
    }

    /**
     * Mints a valid JWT ticket for a given user and thread.
     *
     * @param userId The user identifier (subject).
     * @param threadId The thread identifier.
     * @return Compact serialized JWT token string.
     */
    public String generateToken(String userId, String threadId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(1, ChronoUnit.HOURS);

        return Jwts.builder()
                .header()
                    .keyId(DEV_KID)
                .and()
                .subject(userId)
                .claim("threadId", threadId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }
}
