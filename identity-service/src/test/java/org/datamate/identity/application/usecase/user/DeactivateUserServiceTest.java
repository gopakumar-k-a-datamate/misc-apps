package org.datamate.identity.application.usecase.user;

import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.datamate.identity.identity.application.port.out.user.UserPersistencePort;
import org.datamate.identity.identity.application.usecase.user.DeactivateUserService;
import org.datamate.identity.identity.domain.exception.user.UserNotFoundException;
import org.datamate.identity.identity.domain.model.user.entity.User;
import org.datamate.identity.identity.domain.model.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.context.ApplicationEventPublisher;
import org.datamate.identity.identity.domain.event.user.UserDeactivatedEvent;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeactivateUserServiceTest {

    private UserPersistencePort userPort;
    private ApplicationEventPublisher eventPublisher;
    private Logger log;
    private DeactivateUserService deactivateUserService;

    @BeforeEach
    void setUp() throws Exception {
        userPort = mock(UserPersistencePort.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        log = mock(Logger.class);
        deactivateUserService = new DeactivateUserService(userPort, eventPublisher);

        Field logField = DeactivateUserService.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(deactivateUserService, log);
    }

    @Test
    void shouldDeactivateUserSuccessfullyWhenUserExists() {
        UUID userId = UUID.randomUUID();
        User activeUser = User.reconstitute(
                userId, "active_user", "active@example.com", "+12345",
                "hash", "John", "Doe", "ELLIDER", "EXT-1",
                UserStatus.ACTIVE, new ArrayList<>(), false, 1L, 1L,
                "creator", LocalDateTime.now(), "creator", LocalDateTime.now()
        );

        when(userPort.findById(userId)).thenReturn(Optional.of(activeUser));

        deactivateUserService.deactivateUser(userId, "admin_user");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userPort).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertNotNull(savedUser);
        assertEquals(UserStatus.INACTIVE, savedUser.getStatus());
        assertEquals("admin_user", savedUser.getLastModifiedBy());
        verify(eventPublisher).publishEvent(any(UserDeactivatedEvent.class));
        verify(log).info("Starting deactivation of user ID: {} by admin: {}", userId, "admin_user");
        verify(log).info("Successfully deactivated user ID: {} by admin: {}", userId, "admin_user");
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userPort.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> deactivateUserService.deactivateUser(userId, "admin_user"));

        verify(userPort, never()).save(any(User.class));
        verify(log).info("Starting deactivation of user ID: {} by admin: {}", userId, "admin_user");
        verify(log).error("User deactivation failed. User not found for ID: {}", userId);
    }

    @Test
    void shouldThrowWhenDeactivatingLastActivePolicyAdmin() {
        UUID userId = UUID.randomUUID();
        User adminUser = User.reconstitute(
                userId, "admin@123.com", "admin@123.com", "+12345",
                "hash", "System", "Admin", null, null,
                UserStatus.ACTIVE, List.of("POLICY_ADMIN", "ADMIN"), false, 1L, 1L,
                "system", LocalDateTime.now(), "system", LocalDateTime.now()
        );

        when(userPort.findById(userId)).thenReturn(Optional.of(adminUser));
        when(userPort.countActiveUsersWithRoleExcept("POLICY_ADMIN", userId)).thenReturn(0L);

        assertThrows(
                org.datamate.identity.identity.domain.exception.user.InvalidUserDataException.class,
                () -> deactivateUserService.deactivateUser(userId, "admin_user")
        );

        verify(userPort, never()).save(any(User.class));
    }
}
