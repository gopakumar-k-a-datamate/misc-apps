package org.datamate.identity.application.usecase.auth;

import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.datamate.identity.identity.application.dto.auth.AuthResponse;
import org.datamate.identity.identity.application.dto.auth.RefreshTokenRequest;
import org.datamate.identity.identity.application.port.out.TokenGeneratorPort;
import org.datamate.identity.identity.application.port.out.user.UserPersistencePort;
import org.datamate.identity.identity.application.usecase.auth.RefreshTokenService;
import org.datamate.identity.identity.domain.exception.auth.InvalidRefreshTokenException;
import org.datamate.identity.identity.domain.model.user.entity.User;
import org.datamate.identity.identity.domain.model.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    private TokenGeneratorPort tokenGeneratorPort;
    private UserPersistencePort userPersistencePort;
    private Logger log;
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() throws Exception {
        tokenGeneratorPort = mock(TokenGeneratorPort.class);
        userPersistencePort = mock(UserPersistencePort.class);
        log = mock(Logger.class);
        refreshTokenService = new RefreshTokenService(tokenGeneratorPort, userPersistencePort);

        Field logField = RefreshTokenService.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(refreshTokenService, log);
    }

    @Test
    void shouldRefreshTokenSuccessfullyWhenTokenIsValid() {
        String token = "valid_refresh_token";
        RefreshTokenRequest request = new RefreshTokenRequest(token);
        User user = User.reconstitute(
                UUID.randomUUID(), "test_user", "test@example.com", "+12345",
                "hash", "John", "Doe", "ELLIDER", "EXT-1",
                UserStatus.ACTIVE, new ArrayList<>(), false, 1L, 1L,
                "creator", LocalDateTime.now(), "creator", LocalDateTime.now()
        );

        when(tokenGeneratorPort.isBlacklisted(token)).thenReturn(false);
        when(tokenGeneratorPort.validateToken(token)).thenReturn(true);
        when(tokenGeneratorPort.getTokenType(token)).thenReturn("refresh");
        when(tokenGeneratorPort.getUsernameFromToken(token)).thenReturn("test_user");
        when(userPersistencePort.findByUserName("test_user")).thenReturn(Optional.of(user));
        when(tokenGeneratorPort.generateAccessToken(user)).thenReturn("new_access_token");
        when(tokenGeneratorPort.generateRefreshToken(user)).thenReturn("new_refresh_token");

        AuthResponse response = refreshTokenService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new_access_token", response.accessToken());
        assertEquals("new_refresh_token", response.refreshToken());
        assertEquals("test_user", response.userName());
        assertEquals("test@example.com", response.email());
    }

    @Test
    void shouldThrowInvalidRefreshTokenExceptionWhenTokenIsBlacklisted() {
        String token = "blacklisted_token";
        RefreshTokenRequest request = new RefreshTokenRequest(token);

        when(tokenGeneratorPort.isBlacklisted(token)).thenReturn(true);

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.refreshToken(request));
        verify(tokenGeneratorPort, never()).validateToken(anyString());
    }

    @Test
    void shouldThrowInvalidRefreshTokenExceptionWhenTokenValidationFails() {
        String token = "invalid_token";
        RefreshTokenRequest request = new RefreshTokenRequest(token);

        when(tokenGeneratorPort.isBlacklisted(token)).thenReturn(false);
        when(tokenGeneratorPort.validateToken(token)).thenReturn(false);

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.refreshToken(request));
        verify(tokenGeneratorPort, never()).getTokenType(anyString());
    }

    @Test
    void shouldThrowInvalidRefreshTokenExceptionWhenTokenTypeIsNotRefresh() {
        String token = "access_token_passed_instead";
        RefreshTokenRequest request = new RefreshTokenRequest(token);

        when(tokenGeneratorPort.isBlacklisted(token)).thenReturn(false);
        when(tokenGeneratorPort.validateToken(token)).thenReturn(true);
        when(tokenGeneratorPort.getTokenType(token)).thenReturn("access");

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.refreshToken(request));
        verify(tokenGeneratorPort, never()).getUsernameFromToken(anyString());
    }

    @Test
    void shouldThrowInvalidRefreshTokenExceptionWhenUsernameIsBlank() {
        String token = "no_username_token";
        RefreshTokenRequest request = new RefreshTokenRequest(token);

        when(tokenGeneratorPort.isBlacklisted(token)).thenReturn(false);
        when(tokenGeneratorPort.validateToken(token)).thenReturn(true);
        when(tokenGeneratorPort.getTokenType(token)).thenReturn("refresh");
        when(tokenGeneratorPort.getUsernameFromToken(token)).thenReturn("");

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.refreshToken(request));
        verify(userPersistencePort, never()).findByUserName(anyString());
    }

    @Test
    void shouldThrowInvalidRefreshTokenExceptionWhenUserNotFound() {
        String token = "user_deleted_token";
        RefreshTokenRequest request = new RefreshTokenRequest(token);

        when(tokenGeneratorPort.isBlacklisted(token)).thenReturn(false);
        when(tokenGeneratorPort.validateToken(token)).thenReturn(true);
        when(tokenGeneratorPort.getTokenType(token)).thenReturn("refresh");
        when(tokenGeneratorPort.getUsernameFromToken(token)).thenReturn("deleted_user");
        when(userPersistencePort.findByUserName("deleted_user")).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.refreshToken(request));
    }
}
