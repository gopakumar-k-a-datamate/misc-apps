package org.datamate.identity.application.usecase;

import com.datamate.bedrock.framework.common.ddd.datatype.EntityReference;
import com.datamate.bedrock.framework.common.ddd.datatype.ResourceIdentifier;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.datamate.identity.identity.application.dto.user.CreateUserRequest;
import org.datamate.identity.identity.application.dto.user.UserDto;
import org.datamate.identity.identity.application.mapper.user.UserDtoMapper;
import org.datamate.identity.identity.application.port.out.PasswordEncoderPort;
import org.datamate.identity.identity.application.port.out.SecurityContextPort;
import org.datamate.identity.identity.application.port.out.user.UserPersistencePort;
import org.datamate.identity.identity.application.usecase.user.CreateUserService;
import org.datamate.identity.identity.domain.event.user.UserCreatedEvent;
import org.datamate.identity.identity.domain.exception.user.UserAlreadyExistsException;
import org.datamate.identity.identity.domain.model.user.entity.User;
import org.datamate.identity.identity.domain.model.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import org.datamate.identity.identity.application.port.out.role.RolePersistencePort;
import org.datamate.identity.identity.domain.model.role.entity.Role;
import org.datamate.identity.identity.domain.model.role.enums.RoleStatus;
import org.datamate.identity.identity.domain.exception.role.RoleNotFoundException;
import org.datamate.identity.identity.domain.exception.user.InvalidRoleAssignmentException;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateUserServiceTest {

    private UserPersistencePort userPort;
    private RolePersistencePort rolePort;
    private PasswordEncoderPort passwordEncoderPort;
    private SecurityContextPort securityContextPort;
    private ApplicationEventPublisher eventPublisher;
    private UserDtoMapper userDtoMapper;
    private Logger log;
    private CreateUserService createUserService;

    @BeforeEach
    void setUp() throws Exception {
        userPort = mock(UserPersistencePort.class);
        rolePort = mock(RolePersistencePort.class);
        passwordEncoderPort = mock(PasswordEncoderPort.class);
        securityContextPort = mock(SecurityContextPort.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        userDtoMapper = new UserDtoMapper();
        log = mock(Logger.class);

        createUserService = new CreateUserService(userPort, rolePort, passwordEncoderPort, securityContextPort, eventPublisher, userDtoMapper);

        // Manually inject the logger mock because it's a non-constructor private field (@EnableLogger)
        Field logField = CreateUserService.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(createUserService, log);
    }

    @Test
    void shouldCreateUserSuccessfullyWhenValidCommandProvided() {
        CreateUserRequest command = new CreateUserRequest(
                "jane_doe",
                "jane@example.com",
                "+1987654321",
                "Jane",
                "Doe",
                "secret123",
                "ELLIDER",
                "EXT-12345",
                Collections.emptyList()
        );

        UUID userId = UUID.randomUUID();

        when(userPort.existsByUserName("jane_doe")).thenReturn(false);
        when(userPort.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoderPort.encode("secret123")).thenReturn("hashed_secret123");
        when(securityContextPort.getCurrentUsername()).thenReturn("admin_user");

        User savedUserMock = User.reconstitute(
                userId,
                "jane_doe",
                "jane@example.com",
                "+1987654321",
                "hashed_secret123",
                "Jane",
                "Doe",
                "ELLIDER",
                "EXT-12345",
                UserStatus.ACTIVE,
                Collections.emptyList(),
                true,
                0L,
                1L,
                "admin_user",
                LocalDateTime.now(),
                "admin_user",
                LocalDateTime.now()
        );

        when(userPort.save(any(User.class))).thenReturn(savedUserMock);

        UserDto result = createUserService.createUser(command);

        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals("jane_doe", result.userName());
        assertEquals("jane@example.com", result.email());
        assertEquals("+1987654321", result.phoneNumber());
        assertEquals("Jane", result.firstName());
        assertEquals("Doe", result.lastName());
        assertEquals("ELLIDER", result.referenceSystem());
        assertEquals("EXT-12345", result.referenceValue());
        assertEquals("admin_user", result.createdBy());

        verify(userPort).save(any(User.class));
        verify(eventPublisher).publishEvent(any(UserCreatedEvent.class));
    }

    @Test
    void shouldThrowUserAlreadyExistsExceptionWhenUserNameExists() {
        CreateUserRequest command = new CreateUserRequest(
                "jane_doe",
                "jane@example.com",
                "+1987654321",
                "Jane",
                "Doe",
                "secret123",
                null,
                null,
                Collections.emptyList()
        );

        when(userPort.existsByUserName("jane_doe")).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> createUserService.createUser(command)
        );

        assertEquals("user.alreadyExists", exception.getErrorCode());
        verify(userPort, never()).save(any(User.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowUserAlreadyExistsExceptionWhenEmailExists() {
        CreateUserRequest command = new CreateUserRequest(
                "jane_doe",
                "jane@example.com",
                "+1987654321",
                "Jane",
                "Doe",
                "secret123",
                null,
                null,
                Collections.emptyList()
        );

        when(userPort.existsByUserName("jane_doe")).thenReturn(false);
        when(userPort.existsByEmail("jane@example.com")).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> createUserService.createUser(command)
        );

        assertEquals("user.alreadyExists", exception.getErrorCode());
        verify(userPort, never()).save(any(User.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldCreateUserWithActiveRoles() {
        CreateUserRequest command = new CreateUserRequest(
                "jane_doe",
                "jane@example.com",
                "+1987654321",
                "Jane",
                "Doe",
                "secret123",
                "ELLIDER",
                "EXT-12345",
                List.of("ADMIN")
        );

        UUID userId = UUID.randomUUID();
        EntityReference<UUID> auditRef =
                new EntityReference<>(null, new ResourceIdentifier("system", "admin_user"));

        Role adminRole = Role.reconstitute(
                UUID.randomUUID(), "ADMIN", "Administrator Role", RoleStatus.ACTIVE,
                null, null, 1L, 1L, auditRef, LocalDateTime.now(), auditRef, LocalDateTime.now()
        );

        when(userPort.existsByUserName("jane_doe")).thenReturn(false);
        when(userPort.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoderPort.encode("secret123")).thenReturn("hashed_secret123");
        when(securityContextPort.getCurrentUsername()).thenReturn("admin_user");
        when(rolePort.findAllByNameIn(List.of("ADMIN"))).thenReturn(List.of(adminRole));

        User savedUserMock = User.reconstitute(
                userId,
                "jane_doe",
                "jane@example.com",
                "+1987654321",
                "hashed_secret123",
                "Jane",
                "Doe",
                "ELLIDER",
                "EXT-12345",
                UserStatus.ACTIVE,
                List.of("ADMIN"),
                true,
                0L,
                1L,
                "admin_user",
                LocalDateTime.now(),
                "admin_user",
                LocalDateTime.now()
        );

        when(userPort.save(any(User.class))).thenReturn(savedUserMock);

        UserDto result = createUserService.createUser(command);

        assertNotNull(result);
        assertEquals(List.of("ADMIN"), result.roles());
        verify(rolePort).findAllByNameIn(List.of("ADMIN"));
        verify(userPort).save(any(User.class));
    }

    @Test
    void shouldThrowRoleNotFoundExceptionWhenRoleDoesNotExist() {
        CreateUserRequest command = new CreateUserRequest(
                "jane_doe",
                "jane@example.com",
                "+1987654321",
                "Jane",
                "Doe",
                "secret123",
                "ELLIDER",
                "EXT-12345",
                List.of("NON_EXISTENT")
        );

        when(userPort.existsByUserName("jane_doe")).thenReturn(false);
        when(userPort.existsByEmail("jane@example.com")).thenReturn(false);
        when(rolePort.findAllByNameIn(List.of("NON_EXISTENT"))).thenReturn(List.of());

        assertThrows(RoleNotFoundException.class, () -> createUserService.createUser(command));

        verify(userPort, never()).save(any(User.class));
    }

    @Test
    void shouldThrowInvalidRoleAssignmentExceptionWhenRoleIsInactive() {
        CreateUserRequest command = new CreateUserRequest(
                "jane_doe",
                "jane@example.com",
                "+1987654321",
                "Jane",
                "Doe",
                "secret123",
                "ELLIDER",
                "EXT-12345",
                List.of("INACTIVE_ROLE")
        );

        EntityReference<UUID> auditRef =
                new EntityReference<>(null, new ResourceIdentifier("system", "admin_user"));

        Role inactiveRole = Role.reconstitute(
                UUID.randomUUID(), "INACTIVE_ROLE", "Inactive Role", RoleStatus.INACTIVE,
                null, null, 1L, 1L, auditRef, LocalDateTime.now(), auditRef, LocalDateTime.now()
        );

        when(userPort.existsByUserName("jane_doe")).thenReturn(false);
        when(userPort.existsByEmail("jane@example.com")).thenReturn(false);
        when(rolePort.findAllByNameIn(List.of("INACTIVE_ROLE"))).thenReturn(List.of(inactiveRole));

        assertThrows(InvalidRoleAssignmentException.class, () -> createUserService.createUser(command));

        verify(userPort, never()).save(any(User.class));
    }
}