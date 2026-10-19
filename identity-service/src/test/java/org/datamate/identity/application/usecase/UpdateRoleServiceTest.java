package org.datamate.identity.application.usecase;

import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.datamate.identity.identity.application.dto.role.RoleDto;
import org.datamate.identity.identity.application.dto.role.UpdateRoleRequest;
import org.datamate.identity.identity.application.mapper.role.RoleDtoMapper;
import org.datamate.identity.identity.application.port.out.role.RolePersistencePort;
import org.datamate.identity.identity.application.usecase.role.UpdateRoleService;
import org.datamate.identity.identity.domain.exception.role.RoleAlreadyExistsException;
import org.datamate.identity.identity.domain.exception.role.RoleNotFoundException;
import org.datamate.identity.identity.domain.model.role.entity.Role;
import org.datamate.identity.identity.domain.model.role.enums.RoleStatus;
import org.datamate.identity.identity.domain.event.role.RoleUpdatedEvent;
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

class UpdateRoleServiceTest {

    private RolePersistencePort rolePort;
    private RoleDtoMapper roleDtoMapper;
    private ApplicationEventPublisher eventPublisher;
    private Logger log;
    private UpdateRoleService updateRoleService;

    private final UUID roleId = UUID.randomUUID();
    private final EntityReference<UUID> adminUserRef = new EntityReference<>(
            UUID.randomUUID(),
            new ResourceIdentifier("identity-service", "admin")
    );

    @BeforeEach
    void setUp() throws Exception {
        rolePort = mock(RolePersistencePort.class);
        roleDtoMapper = new RoleDtoMapper();
        eventPublisher = mock(ApplicationEventPublisher.class);
        log = mock(Logger.class);

        updateRoleService = new UpdateRoleService(rolePort, roleDtoMapper, eventPublisher);

        // Manually inject the logger mock because it's a non-constructor private field (@EnableLogger)
        Field logField = UpdateRoleService.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(updateRoleService, log);
    }

    @Test
    void shouldUpdateRoleSuccessfullyWhenValidRequestAndRoleExists() {
        // Arrange
        Role sampleRole = Role.reconstitute(
                roleId,
                "OLD_NAME",
                "Old Description",
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

        UpdateRoleRequest request = new UpdateRoleRequest("NEW_NAME", "New Description");

        when(rolePort.findById(roleId)).thenReturn(Optional.of(sampleRole));
        when(rolePort.existsByNameIgnoreCaseAndIdNot("NEW_NAME", roleId)).thenReturn(false);
        when(rolePort.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        RoleDto result = updateRoleService.updateRole(roleId, request, adminUserRef);

        // Assert
        assertNotNull(result);
        assertEquals(roleId, result.id());
        assertEquals("NEW_NAME", result.name());
        assertEquals("New Description", result.description());
        assertEquals(RoleStatus.ACTIVE, result.status());

        verify(rolePort).findById(roleId);
        verify(rolePort).existsByNameIgnoreCaseAndIdNot("NEW_NAME", roleId);
        verify(rolePort).save(any(Role.class));
        verify(eventPublisher, atLeastOnce()).publishEvent(any(RoleUpdatedEvent.class));
    }

    @Test
    void shouldThrowRoleNotFoundExceptionWhenRoleDoesNotExist() {
        // Arrange
        UpdateRoleRequest request = new UpdateRoleRequest("NEW_NAME", "New Description");
        when(rolePort.findById(roleId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RoleNotFoundException.class, () -> 
            updateRoleService.updateRole(roleId, request, adminUserRef)
        );

        verify(rolePort).findById(roleId);
        verify(rolePort, never()).save(any(Role.class));
    }

    @Test
    void shouldThrowRoleAlreadyExistsExceptionWhenNameDuplicatesAnotherRole() {
        // Arrange
        Role sampleRole = Role.reconstitute(
                roleId,
                "OLD_NAME",
                "Old Description",
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

        UpdateRoleRequest request = new UpdateRoleRequest("DUPLICATED_NAME", "New Description");

        when(rolePort.findById(roleId)).thenReturn(Optional.of(sampleRole));
        when(rolePort.existsByNameIgnoreCaseAndIdNot("DUPLICATED_NAME", roleId)).thenReturn(true);

        // Act & Assert
        assertThrows(RoleAlreadyExistsException.class, () -> 
            updateRoleService.updateRole(roleId, request, adminUserRef)
        );

        verify(rolePort).findById(roleId);
        verify(rolePort).existsByNameIgnoreCaseAndIdNot("DUPLICATED_NAME", roleId);
        verify(rolePort, never()).save(any(Role.class));
    }
}
