package org.datamate.collaboration.security.dev;

import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.datamate.collaboration.security.KeyRegistry;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Date;
import java.util.List;

/**
 * Development-only service to generate signed RS256 JWT tickets for local/dev testing.
 * Strictly active only under the 'local' or 'dev' Spring profiles.
 */
@Service
@Profile({"local", "dev", "default"})
@RequiredArgsConstructor
public class DevTicketService {

    public static final String DEV_ISSUER = "dev-mock-service";
    public static final String DEV_KID = "dev-key-1";

    private final KeyRegistry keyRegistry;
    private KeyPair devKeyPair;

    @PostConstruct
    public void init() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        this.devKeyPair = kpg.generateKeyPair();
        this.keyRegistry.registerKey(DEV_KID, DEV_ISSUER, devKeyPair.getPublic());
    }

    public String generateTicket(String userId, String threadId, List<String> permissions, long validitySeconds) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + (validitySeconds * 1000));

        List<String> perms = (permissions != null && !permissions.isEmpty()) ? permissions : List.of("READ", "WRITE");

        return Jwts.builder()
                .header()
                    .keyId(DEV_KID)
                    .type("JWT")
                .and()
                .subject(userId != null && !userId.isBlank() ? userId : "test-user")
                .issuer(DEV_ISSUER)
                .claim("threadId", threadId)
                .claim("permissions", perms)
                .issuedAt(now)
                .expiration(exp)
                .signWith(devKeyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }
}
