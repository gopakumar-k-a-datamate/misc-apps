package org.datamate.identity.identity.application.dto.auth;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String userName,
        String email
) {}
