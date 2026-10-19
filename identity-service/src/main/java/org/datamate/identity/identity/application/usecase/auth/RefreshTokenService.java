package org.datamate.identity.identity.application.usecase.auth;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import lombok.RequiredArgsConstructor;
import org.datamate.identity.identity.application.dto.auth.AuthResponse;
import org.datamate.identity.identity.application.dto.auth.RefreshTokenRequest;
import org.datamate.identity.identity.application.port.in.auth.RefreshTokenUseCase;
import org.datamate.identity.identity.application.port.out.TokenGeneratorPort;
import org.datamate.identity.identity.application.port.out.user.UserPersistencePort;
import org.datamate.identity.identity.domain.exception.auth.InvalidRefreshTokenException;
import org.datamate.identity.identity.domain.model.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService implements RefreshTokenUseCase {

    @EnableLogger
    private Logger log;

    private final TokenGeneratorPort tokenGeneratorPort;
    private final UserPersistencePort userPersistencePort;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.refreshToken();
        log.info("Processing token refresh request");

        if (tokenGeneratorPort.isBlacklisted(token)) {
            log.warn("Token refresh failed: token is blacklisted");
            throw new InvalidRefreshTokenException();
        }

        if (!tokenGeneratorPort.validateToken(token)) {
            log.warn("Token refresh failed: token validation failed or expired");
            throw new InvalidRefreshTokenException();
        }

        String tokenType = tokenGeneratorPort.getTokenType(token);
        if (!"refresh".equalsIgnoreCase(tokenType)) {
            log.warn("Token refresh failed: expected type 'refresh' but got '{}'", tokenType);
            throw new InvalidRefreshTokenException();
        }

        String username = tokenGeneratorPort.getUsernameFromToken(token);
        if (username == null || username.isBlank()) {
            log.warn("Token refresh failed: could not extract username from token");
            throw new InvalidRefreshTokenException();
        }

        User user = userPersistencePort.findByUserName(username)
                .orElseThrow(() -> {
                    log.warn("Token refresh failed: user '{}' not found", username);
                    return new InvalidRefreshTokenException();
                });

        String newAccessToken = tokenGeneratorPort.generateAccessToken(user);
        String newRefreshToken = tokenGeneratorPort.generateRefreshToken(user);

        log.info("Token refreshed successfully for user '{}'", username);
        return new AuthResponse(newAccessToken, newRefreshToken, user.getUserName(), user.getEmail());
    }
}
