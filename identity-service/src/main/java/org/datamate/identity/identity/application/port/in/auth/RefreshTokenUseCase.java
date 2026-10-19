package org.datamate.identity.identity.application.port.in.auth;

import org.datamate.identity.identity.application.dto.auth.AuthResponse;
import org.datamate.identity.identity.application.dto.auth.RefreshTokenRequest;

public interface RefreshTokenUseCase {
    AuthResponse refreshToken(RefreshTokenRequest request);
}
