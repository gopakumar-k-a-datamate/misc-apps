package org.datamate.collaboration.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service responsible for verifying and parsing JWT Chat Tickets.
 * Strictly adheres to the 7-step token validation pipeline defined in Section 8
 * of the Standalone Chat Engine specification:
 * <ol>
 *   <li>Structural check (exactly 3 Base64URL parts separated by dots)</li>
 *   <li>Algorithm whitelist check (strictly RS256 - reject 'none', 'HS256', etc.)</li>
 *   <li>Key resolution by 'kid'</li>
 *   <li>Asymmetric RSA signature verification</li>
 *   <li>Temporal validation (exp, iat with 30s clock skew)</li>
 *   <li>Required claims verification (sub, threadId, iss)</li>
 *   <li>Context binding (evaluated at REST filter / WebSocket interceptors)</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class JwtVerifier {

    private static final String ALLOWED_ALGORITHM = "RS256";
    private static final long CLOCK_SKEW_SECONDS = 30;

    private final KeyRegistry keyRegistry;

    /**
     * Verifies the given token string and extracts its claims.
     *
     * @param token the raw JWT token.
     * @return the verified Claims.
     * @throws JwtException if the token is invalid, expired, or improperly signed.
     */
    public Claims verify(String token) throws JwtException {
        // Step 1: Structural Check
        if (token == null || token.isBlank()) {
            throw new JwtException("Token must not be null or blank");
        }

        String[] parts = token.trim().split("\\.");
        if (parts.length != 3) {
            throw new JwtException("Invalid JWT structure: token must contain exactly 3 dot-separated parts (header.payload.signature)");
        }

        AtomicReference<String> resolvedKidRef = new AtomicReference<>();

        // Steps 2 & 3: Algorithm check & Key resolution via Locator
        Locator<Key> keyLocator = header -> {
            if (!(header instanceof JwsHeader jwsHeader)) {
                throw new JwtException("Expected a signed JWS Header");
            }
            
            String algorithm = jwsHeader.getAlgorithm();
            if (!ALLOWED_ALGORITHM.equalsIgnoreCase(algorithm)) {
                throw new JwtException("Algorithm not allowed: " + algorithm + ". Only " + ALLOWED_ALGORITHM + " is permitted.");
            }

            String kid = jwsHeader.getKeyId();
            if (kid == null || kid.isBlank()) {
                throw new JwtException("JWT Header is missing required 'kid' claim");
            }

            resolvedKidRef.set(kid);

            return keyRegistry.getKey(kid)
                    .orElseThrow(() -> new JwtException("Unknown kid: " + kid));
        };

        // Steps 4 & 5: RSA Signature Verification & Temporal Validation (exp, iat with clock skew)
        Claims claims = Jwts.parser()
                .keyLocator(keyLocator)
                .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Step 6: Claims Validation
        String sub = claims.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new JwtException("JWT Ticket is missing 'sub' (subject) claim");
        }

        String threadId = claims.get("threadId", String.class);
        if (threadId == null || threadId.isBlank()) {
            throw new JwtException("JWT Ticket is missing 'threadId' claim");
        }

        String iss = claims.getIssuer();
        if (iss == null || iss.isBlank()) {
            throw new JwtException("JWT Ticket is missing 'iss' (issuer) claim");
        }

        if (!keyRegistry.isKnownIssuer(iss)) {
            throw new JwtException("JWT Ticket has unknown or unregistered issuer: " + iss);
        }

        // Cross-verify with kid's registered issuer if available
        String kid = resolvedKidRef.get();
        if (kid != null) {
            Optional<String> registeredIssuer = keyRegistry.getIssuerForKid(kid);
            if (registeredIssuer.isPresent() && !registeredIssuer.get().equalsIgnoreCase(iss)) {
                throw new JwtException("Issuer mismatch: token 'iss' [" + iss + "] does not match registered issuer [" + registeredIssuer.get() + "] for kid [" + kid + "]");
            }
        }

        return claims;
    }
}
