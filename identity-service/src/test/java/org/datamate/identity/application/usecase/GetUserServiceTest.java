package org.datamate.identity.application.usecase;

import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.datamate.identity.identity.application.dto.user.UserDto;
import org.datamate.identity.identity.application.mapper.user.UserDtoMapper;
import org.datamate.identity.identity.application.port.out.user.UserPersistencePort;
import org.datamate.identity.identity.application.usecase.user.GetUserService;
import org.datamate.identity.identity.domain.exception.user.UserNotFoundException;
import org.datamate.identity.identity.domain.model.user.entity.User;
import org.datamate.identity.identity.domain.model.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetUserServiceTest {

    private UserPersistencePort userPort;
    private UserDtoMapper userDtoMapper;
    private Logger log;
    private GetUserService getUserService;

    @BeforeEach
    void setUp() throws Exception {
        userPort = mock(UserPersistencePort.class);
        userDtoMapper = new UserDtoMapper();
        log = mock(Logger.class);

        getUserService = new GetUserService(userPort, userDtoMapper);

        // Manually inject the logger mock because it's a non-constructor private field (@EnableLogger)
        Field logField = GetUserService.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(getUserService, log);
    }

    @Test
    void shouldReturnUserDetailsSuccessfullyWhenUserExists() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User sampleUser = User.reconstitute(
                userId,
                "john_doe",
                "john@example.com",
                "+1234567890",
                "password_hash",
                "John",
                "Doe",
                "ELLIDER",
                "EXT-12345",
                UserStatus.ACTIVE,
                List.of("USER"),
                false,
                0L,
                1L,
                "admin",
                LocalDateTime.now(),
                "admin",
                LocalDateTime.now()
        );

        when(userPort.findById(userId)).thenReturn(Optional.of(sampleUser));

        // Act
        UserDto result = getUserService.getUserById(userId);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals("john_doe", result.userName());
        assertEquals("john@example.com", result.email());
        assertEquals("John", result.firstName());
        assertEquals("Doe", result.lastName());
        assertEquals(UserStatus.ACTIVE, result.status());
        assertEquals(List.of("USER"), result.roles());

        verify(userPort).findById(userId);
        verify(log).info("Starting retrieval of user details for ID: {}", userId);
        verify(log).info("Successfully retrieved user details for ID: {}", userId);
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userPort.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> getUserService.getUserById(userId));

        verify(userPort).findById(userId);
        verify(log).info("Starting retrieval of user details for ID: {}", userId);
        verify(log).error("User details retrieval failed. User not found for ID: {}", userId);
    }
}
