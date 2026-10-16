package org.datamate.collaboration.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtVerifierTest {

    private InMemoryKeyRegistry keyRegistry;
    private JwtVerifier jwtVerifier;
    private KeyPair keyPair;

    private static final String KID = "restaurant-key-v1";
    private static final String ISSUER = "restaurant-service";

    @BeforeEach
    void setUp() throws Exception {
        keyRegistry = new InMemoryKeyRegistry(null);
        jwtVerifier = new JwtVerifier(keyRegistry);

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        keyPair = keyGen.generateKeyPair();

        keyRegistry.registerKey(KID, ISSUER, keyPair.getPublic());
    }

    private String createValidToken(String sub, String threadId, List<String> permissions, Instant now, Instant exp) {
        return Jwts.builder()
                .header()
                    .keyId(KID)
                    .type("JWT")
                .and()
                .issuer(ISSUER)
                .subject(sub)
                .claim("threadId", threadId)
                .claim("permissions", permissions)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    @Test
    @DisplayName("Step 1-6: Valid RS256 token verification should successfully extract claims")
    void testVerify_ValidRS256Token_ShouldSucceed() {
        Instant now = Instant.now();
        Instant exp = now.plus(5, ChronoUnit.MINUTES);
        String threadId = UUID.randomUUID().toString();

        String token = createValidToken("john", threadId, List.of("READ", "WRITE"), now, exp);

        Claims claims = jwtVerifier.verify(token);

        assertThat(claims.getSubject()).isEqualTo("john");
        assertThat(claims.get("threadId", String.class)).isEqualTo(threadId);
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
    }

    @Test
    @DisplayName("Step 1: Structural check - Token must have exactly 3 dot-separated parts")
    void testVerify_MalformedTokenStructure_ShouldThrowJwtException() {
        assertThatThrownBy(() -> jwtVerifier.verify("header.payload"))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Invalid JWT structure");

        assertThatThrownBy(() -> jwtVerifier.verify(null))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Token must not be null or blank");

        assertThatThrownBy(() -> jwtVerifier.verify("   "))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Token must not be null or blank");
    }

    @Test
    @DisplayName("Step 2: Algorithm check - Strictly whitelist RS256; reject HS256 algorithm confusion")
    void testVerify_Hs256Token_ShouldThrowJwtException() {
        SecretKey secretKey = Keys.hmacShaKeyFor("a-very-secret-key-that-is-at-least-256-bits-long!".getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();

        String hs256Token = Jwts.builder()
                .header()
                    .keyId(KID)
                    .type("JWT")
                .and()
                .issuer(ISSUER)
                .subject("attacker")
                .claim("threadId", "thread-123")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(5, ChronoUnit.MINUTES)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtVerifier.verify(hs256Token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Algorithm not allowed: HS256");
    }

    @Test
    @DisplayName("Step 2: Algorithm check - Reject 'alg: none' critical vulnerability")
    void testVerify_AlgNoneToken_ShouldThrowJwtException() {
        // Base64URL header with alg: none
        String header = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0";
        String payload = "eyJzdWIiOiJhdHRhY2tlciIsImlzcyI6InJlc3RhdXJhbnQtc2VydmljZSIsInRocmVhZElkIjoidGhyZWFkLTEyMyJ9";
        String noneToken = header + "." + payload + ".";

        assertThatThrownBy(() -> jwtVerifier.verify(noneToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Step 3: Key resolution - Unknown kid should throw JwtException")
    void testVerify_UnknownKid_ShouldThrowJwtException() {
        Instant now = Instant.now();
        String tokenWithUnknownKid = Jwts.builder()
                .header()
                    .keyId("unknown-key-999")
                    .type("JWT")
                .and()
                .issuer(ISSUER)
                .subject("john")
                .claim("threadId", "thread-123")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(5, ChronoUnit.MINUTES)))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        assertThatThrownBy(() -> jwtVerifier.verify(tokenWithUnknownKid))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Unknown kid: unknown-key-999");
    }

    @Test
    @DisplayName("Step 4: Signature check - Tampered payload should fail verification")
    void testVerify_TamperedPayload_ShouldThrowJwtException() {
        Instant now = Instant.now();
        String token = createValidToken("john", "original-thread", List.of("READ"), now, now.plus(5, ChronoUnit.MINUTES));

        // Tamper with payload (middle part)
        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + ".eyJzdWIiOiJqb2huIiwidGhyZWFkSWQiOiJ0YW1wZXJlZC10aHJlYWQifQ." + parts[2];

        assertThatThrownBy(() -> jwtVerifier.verify(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Step 5: Temporal check - Expired token beyond 30s clock skew should be rejected")
    void testVerify_ExpiredToken_ShouldThrowJwtException() {
        Instant past = Instant.now().minus(10, ChronoUnit.MINUTES);
        Instant expiredAt = Instant.now().minus(5, ChronoUnit.MINUTES);

        String expiredToken = createValidToken("john", "thread-123", List.of("READ"), past, expiredAt);

        assertThatThrownBy(() -> jwtVerifier.verify(expiredToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Step 6: Required claims - Missing 'threadId' claim should be rejected")
    void testVerify_MissingThreadId_ShouldThrowJwtException() {
        Instant now = Instant.now();
        String tokenNoThread = Jwts.builder()
                .header()
                    .keyId(KID)
                    .type("JWT")
                .and()
                .issuer(ISSUER)
                .subject("john")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(5, ChronoUnit.MINUTES)))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        assertThatThrownBy(() -> jwtVerifier.verify(tokenNoThread))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("missing 'threadId' claim");
    }

    @Test
    @DisplayName("Step 6: Required claims - Missing 'sub' claim should be rejected")
    void testVerify_MissingSub_ShouldThrowJwtException() {
        Instant now = Instant.now();
        String tokenNoSub = Jwts.builder()
                .header()
                    .keyId(KID)
                    .type("JWT")
                .and()
                .issuer(ISSUER)
                .claim("threadId", "thread-123")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(5, ChronoUnit.MINUTES)))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        assertThatThrownBy(() -> jwtVerifier.verify(tokenNoSub))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("missing 'sub' (subject) claim");
    }

    @Test
    @DisplayName("Step 6: Required claims - Unknown issuer should be rejected when registry has registered issuers")
    void testVerify_UnknownIssuer_ShouldThrowJwtException() {
        Instant now = Instant.now();
        String tokenWrongIssuer = Jwts.builder()
                .header()
                    .keyId(KID)
                    .type("JWT")
                .and()
                .issuer("unregistered-rogue-service")
                .subject("john")
                .claim("threadId", "thread-123")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(5, ChronoUnit.MINUTES)))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        assertThatThrownBy(() -> jwtVerifier.verify(tokenWrongIssuer))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("unregistered issuer");
    }
}
