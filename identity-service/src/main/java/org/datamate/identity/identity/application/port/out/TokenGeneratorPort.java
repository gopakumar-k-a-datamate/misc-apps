package org.datamate.identity.identity.application.port.out;

import org.datamate.identity.identity.domain.model.user.entity.User;

public interface TokenGeneratorPort {
    String generateAccessToken(User user);
    String generateRefreshToken(User user);
    void invalidateToken(String token);
    boolean isBlacklisted(String token);
    boolean validateToken(String token);
    String getUsernameFromToken(String token);
    String getTokenType(String token);
}
