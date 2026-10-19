package org.datamate.identity.identity.application.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is mandatory")
        String refreshToken
) {}
