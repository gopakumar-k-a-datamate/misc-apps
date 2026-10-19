package org.datamate.identity.application.usecase.user;

import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.datamate.identity.identity.application.port.out.user.UserPersistencePort;
import org.datamate.identity.identity.application.usecase.user.ActivateUserService;
import org.datamate.identity.identity.domain.exception.user.UserNotFoundException;
import org.datamate.identity.identity.domain.model.user.entity.User;
import org.datamate.identity.identity.domain.model.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.context.ApplicationEventPublisher;
import org.datamate.identity.identity.domain.event.user.UserActivatedEvent;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActivateUserServiceTest {

    private UserPersistencePort userPort;
    private ApplicationEventPublisher eventPublisher;
    private Logger log;
    private ActivateUserService activateUserService;

    @BeforeEach
    void setUp() throws Exception {
        userPort = mock(UserPersistencePort.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        log = mock(Logger.class);
        activateUserService = new ActivateUserService(userPort, eventPublisher);

        Field logField = ActivateUserService.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(activateUserService, log);
    }

    @Test
    void shouldActivateUserSuccessfullyWhenUserExists() {
        UUID userId = UUID.randomUUID();
        User inactiveUser = User.reconstitute(
                userId, "inactive_user", "inactive@example.com", "+12345",
                "hash", "John", "Doe", "ELLIDER", "EXT-1",
                UserStatus.INACTIVE, new ArrayList<>(), false, 1L, 1L,
                "creator", LocalDateTime.now(), "creator", LocalDateTime.now()
        );

        when(userPort.findById(userId)).thenReturn(Optional.of(inactiveUser));

        activateUserService.activateUser(userId, "admin_user");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userPort).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertNotNull(savedUser);
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
        assertEquals("admin_user", savedUser.getLastModifiedBy());
        verify(eventPublisher).publishEvent(any(UserActivatedEvent.class));
        verify(log).info("Starting activation of user ID: {} by admin: {}", userId, "admin_user");
        verify(log).info("Successfully activated user ID: {} by admin: {}", userId, "admin_user");
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userPort.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> activateUserService.activateUser(userId, "admin_user"));

        verify(userPort, never()).save(any(User.class));
        verify(log).info("Starting activation of user ID: {} by admin: {}", userId, "admin_user");
        verify(log).error("User activation failed. User not found for ID: {}", userId);
    }
}
