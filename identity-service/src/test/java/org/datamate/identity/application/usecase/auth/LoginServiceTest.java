package org.datamate.identity.application.usecase.auth;

import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.datamate.identity.identity.application.dto.auth.AuthResponse;
import org.datamate.identity.identity.application.dto.auth.LoginRequest;
import org.datamate.identity.identity.application.port.out.PasswordEncoderPort;
import org.datamate.identity.identity.application.port.out.TokenGeneratorPort;
import org.datamate.identity.identity.application.port.out.user.UserPersistencePort;
import org.datamate.identity.identity.application.usecase.auth.LoginService;
import org.datamate.identity.identity.domain.exception.auth.InvalidCredentialsException;
import org.datamate.identity.identity.domain.exception.user.UserInactiveException;
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

class LoginServiceTest {

    private UserPersistencePort userPersistencePort;
    private PasswordEncoderPort passwordEncoderPort;
    private TokenGeneratorPort tokenGeneratorPort;
    private Logger log;
    private LoginService loginService;

    @BeforeEach
    void setUp() throws Exception {
        userPersistencePort = mock(UserPersistencePort.class);
        passwordEncoderPort = mock(PasswordEncoderPort.class);
        tokenGeneratorPort = mock(TokenGeneratorPort.class);
        log = mock(Logger.class);

        loginService = new LoginService(userPersistencePort, passwordEncoderPort, tokenGeneratorPort);

        Field logField = LoginService.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(loginService, log);
    }

    @Test
    void shouldLoginSuccessfullyWhenUserIsActive() {
        LoginRequest request = new LoginRequest("test_user", "password123");
        User user = User.reconstitute(
                UUID.randomUUID(), "test_user", "test@example.com", "+12345",
                "hash", "John", "Doe", "ELLIDER", "EXT-1",
                UserStatus.ACTIVE, new ArrayList<>(), false, 1L, 1L,
                "creator", LocalDateTime.now(), "creator", LocalDateTime.now()
        );

        when(userPersistencePort.findByUserNameOrEmail("test_user", "test_user")).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("password123", "hash")).thenReturn(true);
        when(tokenGeneratorPort.generateAccessToken(user)).thenReturn("access_token");
        when(tokenGeneratorPort.generateRefreshToken(user)).thenReturn("refresh_token");

        AuthResponse response = loginService.login(request);

        assertNotNull(response);
        assertEquals("access_token", response.accessToken());
        assertEquals("refresh_token", response.refreshToken());
        assertEquals("test_user", response.userName());
    }

    @Test
    void shouldFailLoginWhenUserIsInactive() {
        LoginRequest request = new LoginRequest("test_user", "password123");
        User user = User.reconstitute(
                UUID.randomUUID(), "test_user", "test@example.com", "+12345",
                "hash", "John", "Doe", "ELLIDER", "EXT-1",
                UserStatus.INACTIVE, new ArrayList<>(), false, 1L, 1L,
                "creator", LocalDateTime.now(), "creator", LocalDateTime.now()
        );

        when(userPersistencePort.findByUserNameOrEmail("test_user", "test_user")).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("password123", "hash")).thenReturn(true);

        assertThrows(UserInactiveException.class, () -> loginService.login(request));
        verify(tokenGeneratorPort, never()).generateAccessToken(any());
    }

    @Test
    void shouldFailLoginWhenPasswordIsIncorrect() {
        LoginRequest request = new LoginRequest("test_user", "password123");
        User user = User.reconstitute(
                UUID.randomUUID(), "test_user", "test@example.com", "+12345",
                "hash", "John", "Doe", "ELLIDER", "EXT-1",
                UserStatus.ACTIVE, new ArrayList<>(), false, 1L, 1L,
                "creator", LocalDateTime.now(), "creator", LocalDateTime.now()
        );

        when(userPersistencePort.findByUserNameOrEmail("test_user", "test_user")).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("password123", "hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> loginService.login(request));
    }

    @Test
    void shouldFailLoginWhenUserNotFound() {
        LoginRequest request = new LoginRequest("non_existent", "password123");

        when(userPersistencePort.findByUserNameOrEmail("non_existent", "non_existent")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> loginService.login(request));
    }

    @Test
    void shouldLogoutSuccessfully() {
        String authHeader = "Bearer jwt_token";

        loginService.logout(authHeader);

        verify(tokenGeneratorPort).invalidateToken("jwt_token");
    }
}
