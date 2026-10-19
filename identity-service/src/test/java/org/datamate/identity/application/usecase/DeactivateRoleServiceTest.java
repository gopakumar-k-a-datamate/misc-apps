package org.datamate.identity.application.usecase;

import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.datamate.identity.identity.application.port.out.role.RolePersistencePort;
import org.datamate.identity.identity.application.usecase.role.DeactivateRoleService;
import org.datamate.identity.identity.domain.exception.role.RoleNotFoundException;
import org.datamate.identity.identity.domain.exception.role.InvalidRoleDataException;
import org.datamate.identity.identity.domain.model.role.entity.Role;
import org.datamate.identity.identity.domain.model.role.enums.RoleStatus;
import org.datamate.identity.identity.domain.event.role.RoleDeactivatedEvent;
import com.datamate.bedrock.framework.common.ddd.datatype.EntityReference;
import com.datamate.bedrock.framework.common.ddd.datatype.ResourceIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class DeactivateRoleServiceTest {

    private RolePersistencePort rolePort;
    private ApplicationEventPublisher eventPublisher;
    private Logger log;
    private DeactivateRoleService deactivateRoleService;

    private final UUID roleId = UUID.randomUUID();
    private final EntityReference<UUID> adminUserRef = new EntityReference<>(
            UUID.randomUUID(),
            new ResourceIdentifier("identity-service", "admin")
    );

    @BeforeEach
    void setUp() throws Exception {
        rolePort = mock(RolePersistencePort.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        log = mock(Logger.class);

        deactivateRoleService = new DeactivateRoleService(rolePort, eventPublisher);

        Field logField = DeactivateRoleService.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(deactivateRoleService, log);
    }

    @Test
    void shouldDeactivateRoleSuccessfullyWhenRoleExistsAndIsActive() {
        // Arrange
        Role sampleRole = Role.reconstitute(
                roleId,
                "TEST_ROLE",
                "Test Description",
                RoleStatus.ACTIVE,
                null,
                null,
                1L,
                1L,
                adminUserRef,
                LocalDateTime.now(),
                adminUserRef,
                LocalDateTime.now()
        );

        when(rolePort.findById(roleId)).thenReturn(Optional.of(sampleRole));
        when(rolePort.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        deactivateRoleService.deactivateRole(roleId, adminUserRef);

        // Assert
        verify(rolePort).findById(roleId);
        verify(rolePort).save(argThat(role -> role.getStatus() == RoleStatus.INACTIVE));
        verify(eventPublisher).publishEvent(any(RoleDeactivatedEvent.class));
    }

    @Test
    void shouldThrowInvalidRoleDataExceptionWhenRoleIsAlreadyInactive() {
        // Arrange
        Role sampleRole = Role.reconstitute(
                roleId,
                "TEST_ROLE",
                "Test Description",
                RoleStatus.INACTIVE,
                null,
                null,
                1L,
                1L,
                adminUserRef,
                LocalDateTime.now(),
                adminUserRef,
                LocalDateTime.now()
        );

        when(rolePort.findById(roleId)).thenReturn(Optional.of(sampleRole));

        // Act & Assert
        assertThrows(InvalidRoleDataException.class, () -> deactivateRoleService.deactivateRole(roleId, adminUserRef));
        verify(rolePort).findById(roleId);
        verify(rolePort, never()).save(any(Role.class));
        verify(eventPublisher, never()).publishEvent(any(RoleDeactivatedEvent.class));
    }

    @Test
    void shouldThrowRoleNotFoundExceptionWhenRoleDoesNotExist() {
        // Arrange
        when(rolePort.findById(roleId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RoleNotFoundException.class, () -> deactivateRoleService.deactivateRole(roleId, adminUserRef));
        verify(rolePort).findById(roleId);
        verify(rolePort, never()).save(any(Role.class));
    }
}
