package org.datamate.collaboration.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Adapter providing test tokens for frontend and local integration testing.
 * Strictly available only on dev, local, and test profiles.
 * Follows the context path convention: /api/{version}/{module_name}/...
 */
@RestController
@RequestMapping({"/api/v1/collaboration/dev", "/api/v1/dev"})
@Profile({"dev", "local", "test", "default"})
@RequiredArgsConstructor
public class DevTokenController {

    private final DevTokenService devTokenService;

    /**
     * Endpoint to generate a valid ticket for a specific user and thread.
     * Example: GET /api/v1/collaboration/dev/token?userId=user123&threadId=thread456
     * Also supports custom permissions: &permissions=READ,WRITE
     */
    @GetMapping("/token")
    public String generateTestToken(
            @RequestParam String userId,
            @RequestParam String threadId,
            @RequestParam(required = false) List<String> permissions) {
        if (permissions != null && !permissions.isEmpty()) {
            return devTokenService.generateToken(userId, threadId, permissions);
        }
        return devTokenService.generateToken(userId, threadId);
    }
}
