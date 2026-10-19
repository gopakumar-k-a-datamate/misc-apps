package org.datamate.collaboration.security.dev;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Development-only REST controller to mint test JWT tickets for Postman, Swagger, or manual testing.
 * Strictly active only under 'local' or 'dev' profiles.
 */
@CrossOrigin
@RestController
@RequestMapping("/api/v1/collaboration/dev")
@Profile({"local", "dev", "default"})
@RequiredArgsConstructor
public class DevTicketController {

    private final DevTicketService devTicketService;

    @PostMapping("/tickets")
    public ResponseEntity<Map<String, Object>> createTicket(@RequestBody(required = false) DevTicketRequest request) {
        String threadId = (request != null && request.getThreadId() != null && !request.getThreadId().isBlank())
                ? request.getThreadId()
                : UUID.randomUUID().toString();
        String userId = (request != null && request.getUserId() != null && !request.getUserId().isBlank())
                ? request.getUserId()
                : "test-user";
        List<String> permissions = (request != null && request.getPermissions() != null && !request.getPermissions().isEmpty())
                ? request.getPermissions()
                : List.of("READ", "WRITE");
        long ttl = (request != null && request.getTtlSeconds() > 0)
                ? request.getTtlSeconds()
                : 3600;

        String ticket = devTicketService.generateTicket(userId, threadId, permissions, ttl);

        return ResponseEntity.ok(Map.of(
                "ticket", ticket,
                "userId", userId,
                "threadId", threadId,
                "permissions", permissions,
                "expiresAt", Instant.now().plusSeconds(ttl).toString(),
                "authorizationHeader", "Bearer " + ticket,
                "webSocketUrl", "ws://localhost:8085/ws?ticket=" + ticket
        ));
    }

    @GetMapping("/ticket")
    public ResponseEntity<Map<String, Object>> getTicket(
            @RequestParam(required = false) String threadId,
            @RequestParam(required = false, defaultValue = "test-user") String userId,
            @RequestParam(required = false, defaultValue = "READ,WRITE") String permissions,
            @RequestParam(required = false, defaultValue = "3600") long ttlSeconds) {

        String effectiveThreadId = (threadId != null && !threadId.isBlank()) ? threadId : UUID.randomUUID().toString();
        List<String> perms = List.of(permissions.split(","));

        String ticket = devTicketService.generateTicket(userId, effectiveThreadId, perms, ttlSeconds);

        return ResponseEntity.ok(Map.of(
                "ticket", ticket,
                "userId", userId,
                "threadId", effectiveThreadId,
                "permissions", perms,
                "expiresAt", Instant.now().plusSeconds(ttlSeconds).toString(),
                "authorizationHeader", "Bearer " + ticket,
                "webSocketUrl", "ws://localhost:8085/ws?ticket=" + ticket
        ));
    }

    @Getter
    @Setter
    public static class DevTicketRequest {
        private String userId;
        private String threadId;
        private List<String> permissions;
        private long ttlSeconds;
    }
}
