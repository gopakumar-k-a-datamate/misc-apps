package org.datamate.collaboration.security.dev;

import io.jsonwebtoken.Claims;
import org.datamate.collaboration.security.InMemoryKeyRegistry;
import org.datamate.collaboration.security.JwtVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DevTicketServiceTest {

    private InMemoryKeyRegistry keyRegistry;
    private JwtVerifier jwtVerifier;
    private DevTicketService devTicketService;

    @BeforeEach
    void setUp() throws Exception {
        keyRegistry = new InMemoryKeyRegistry(null);
        jwtVerifier = new JwtVerifier(keyRegistry);
        devTicketService = new DevTicketService(keyRegistry);
        devTicketService.init();
    }

    @Test
    @DisplayName("DevTicketService should generate a valid RS256 ticket verifiable by JwtVerifier")
    void testGenerateValidTicket() {
        String threadId = UUID.randomUUID().toString();
        String ticket = devTicketService.generateTicket("user-1", threadId, List.of("READ", "WRITE"), 3600);

        assertThat(ticket).isNotBlank();

        Claims claims = jwtVerifier.verify(ticket);

        assertThat(claims.getSubject()).isEqualTo("user-1");
        assertThat(claims.get("threadId", String.class)).isEqualTo(threadId);
        assertThat(claims.getIssuer()).isEqualTo(DevTicketService.DEV_ISSUER);

        @SuppressWarnings("unchecked")
        List<String> perms = (List<String>) claims.get("permissions");
        assertThat(perms).containsExactlyInAnyOrder("READ", "WRITE");
    }
}
