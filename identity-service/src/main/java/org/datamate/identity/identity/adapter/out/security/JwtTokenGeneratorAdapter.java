package org.datamate.identity.identity.adapter.out.security;

import com.datamate.bedrock.framework.common.security.jwt.service.JwtTokenService;
import com.datamate.bedrock.framework.common.security.vo.UserDetails;
import lombok.RequiredArgsConstructor;
import org.datamate.identity.identity.application.port.out.TokenGeneratorPort;
import org.datamate.identity.identity.domain.model.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class JwtTokenGeneratorAdapter implements TokenGeneratorPort {

    private final JwtTokenService jwtTokenService;
    private final ConcurrentHashMap<String, Boolean> tokenBlacklist = new ConcurrentHashMap<>();

    @Override
    public String generateAccessToken(User user) {
        UserDetails details = UserDetails.of(
                user.getId().toString(),
                user.getUserName(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoles()
        );
        return jwtTokenService.generateAccessToken(details);
    }

    @Override
    public String generateRefreshToken(User user) {
        UserDetails details = UserDetails.of(
                user.getId().toString(),
                user.getUserName(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoles()
        );
        return jwtTokenService.generateRefreshToken(details);
    }

    @Override
    public void invalidateToken(String token) {
        tokenBlacklist.put(token, Boolean.TRUE);
    }

    @Override
    public boolean isBlacklisted(String token) {
        return tokenBlacklist.containsKey(token);
    }

    @Override
    public boolean validateToken(String token) {
        return jwtTokenService.validateToken(token).isValid();
    }

    @Override
    public String getUsernameFromToken(String token) {
        return jwtTokenService.getUsernameFromToken(token);
    }

    @Override
    public String getTokenType(String token) {
        return jwtTokenService.getTokenType(token);
    }
}
