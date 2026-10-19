package org.datamate.identity.application.usecase.user;

import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.datamate.identity.identity.application.dto.user.UpdateUserRequest;
import org.datamate.identity.identity.application.dto.user.UserDto;
import org.datamate.identity.identity.application.mapper.user.UserDtoMapper;
import org.datamate.identity.identity.application.port.out.user.UserPersistencePort;
import org.datamate.identity.identity.application.usecase.user.UpdateUserService;
import org.datamate.identity.identity.domain.exception.user.UserAlreadyExistsException;
import org.datamate.identity.identity.domain.exception.user.UserNotFoundException;
import org.datamate.identity.identity.domain.model.user.entity.User;
import org.datamate.identity.identity.domain.event.user.UserInformationUpdatedEvent;
import org.datamate.identity.identity.domain.model.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UpdateUserServiceTest {

    private UserPersistencePort userPort;
    private UserDtoMapper userDtoMapper;
    private ApplicationEventPublisher eventPublisher;
    private Logger log;
    private UpdateUserService updateUserService;

    @BeforeEach
    void setUp() throws Exception {
        userPort = mock(UserPersistencePort.class);
        userDtoMapper = mock(UserDtoMapper.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        log = mock(Logger.class);
        updateUserService = new UpdateUserService(userPort, userDtoMapper, eventPublisher);

        Field logField = UpdateUserService.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(updateUserService, log);
    }

    @Test
    void shouldUpdateUserSuccessfullyWhenUserExistsAndEmailIsUnique() {
        UUID userId = UUID.randomUUID();
        User existingUser = User.reconstitute(
                userId, "test_user", "old@example.com", "+12345",
                "hash", "John", "Doe", "ELLIDER", "EXT-1",
                UserStatus.ACTIVE, new ArrayList<>(), false, 1L, 1L,
                "creator", LocalDateTime.now(), "creator", LocalDateTime.now()
        );

        UpdateUserRequest request = new UpdateUserRequest(
                "new_test_user", "new@example.com", "+54321", "Jane", "Smith", "ELLIDER", "EXT-2"
        );

        when(userPort.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userPort.existsByUserNameAndIdNot("new_test_user", userId)).thenReturn(false);
        when(userPort.existsByEmailAndIdNot("new@example.com", userId)).thenReturn(false);

        User updatedUser = existingUser.updateInformation(
                request.userName(), request.email(), request.phoneNumber(), request.firstName(), request.lastName(),
                request.referenceSystem(), request.referenceValue(), "admin_user"
        );

        when(userPort.save(any(User.class))).thenReturn(updatedUser);
        UserDto expectedDto = new UserDto(
                userId, "new_test_user", "new@example.com", "+54321",
                "Jane", "Smith", "ELLIDER", "EXT-2", "creator",
                LocalDateTime.now(), UserStatus.ACTIVE, new ArrayList<>(), false
        );
        when(userDtoMapper.toDto(any(User.class))).thenReturn(expectedDto);

        UserDto result = updateUserService.updateUser(userId, request, "admin_user");

        assertNotNull(result);
        assertEquals("new_test_user", result.userName());
        assertEquals("new@example.com", result.email());
        assertEquals("Jane", result.firstName());
        assertEquals("Smith", result.lastName());

        verify(eventPublisher).publishEvent(any(UserInformationUpdatedEvent.class));
        verify(userPort).save(any(User.class));
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UpdateUserRequest request = new UpdateUserRequest(
                "new_test_user", "new@example.com", "+54321", "Jane", "Smith", "ELLIDER", "EXT-2"
        );

        when(userPort.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> updateUserService.updateUser(userId, request, "admin_user"));

        verify(userPort, never()).save(any(User.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowUserAlreadyExistsExceptionWhenUserNameIsTakenByAnotherUser() {
        UUID userId = UUID.randomUUID();
        User existingUser = User.reconstitute(
                userId, "test_user", "old@example.com", "+12345",
                "hash", "John", "Doe", "ELLIDER", "EXT-1",
                UserStatus.ACTIVE, new ArrayList<>(), false, 1L, 1L,
                "creator", LocalDateTime.now(), "creator", LocalDateTime.now()
        );

        UpdateUserRequest request = new UpdateUserRequest(
                "taken_user", "new@example.com", "+54321", "Jane", "Smith", "ELLIDER", "EXT-2"
        );

        when(userPort.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userPort.existsByUserNameAndIdNot("taken_user", userId)).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> updateUserService.updateUser(userId, request, "admin_user"));

        verify(userPort, never()).save(any(User.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowUserAlreadyExistsExceptionWhenEmailIsTakenByAnotherUser() {
        UUID userId = UUID.randomUUID();
        User existingUser = User.reconstitute(
                userId, "test_user", "old@example.com", "+12345",
                "hash", "John", "Doe", "ELLIDER", "EXT-1",
                UserStatus.ACTIVE, new ArrayList<>(), false, 1L, 1L,
                "creator", LocalDateTime.now(), "creator", LocalDateTime.now()
        );

        UpdateUserRequest request = new UpdateUserRequest(
                "new_test_user", "taken@example.com", "+54321", "Jane", "Smith", "ELLIDER", "EXT-2"
        );

        when(userPort.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userPort.existsByUserNameAndIdNot("new_test_user", userId)).thenReturn(false);
        when(userPort.existsByEmailAndIdNot("taken@example.com", userId)).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> updateUserService.updateUser(userId, request, "admin_user"));

        verify(userPort, never()).save(any(User.class));
        verify(eventPublisher, never()).publishEvent(any());
    }
}
