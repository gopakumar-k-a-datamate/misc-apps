package org.datamate.identity.application.usecase;

import com.datamate.bedrock.framework.common.ddd.datatype.EntityReference;
import com.datamate.bedrock.framework.common.ddd.datatype.ResourceIdentifier;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.datamate.identity.identity.application.dto.role.RoleDto;
import org.datamate.identity.identity.application.mapper.role.RoleDtoMapper;
import org.datamate.identity.identity.application.port.out.role.RolePersistencePort;
import org.datamate.identity.identity.application.usecase.role.GetRoleService;
import org.datamate.identity.identity.domain.exception.role.RoleNotFoundException;
import org.datamate.identity.identity.domain.model.role.entity.Role;
import org.datamate.identity.identity.domain.model.role.enums.RoleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetRoleServiceTest {

    private RolePersistencePort rolePort;
    private RoleDtoMapper roleDtoMapper;
    private Logger log;
    private GetRoleService getRoleService;

    @BeforeEach
    void setUp() throws Exception {
        rolePort = mock(RolePersistencePort.class);
        roleDtoMapper = new RoleDtoMapper();
        log = mock(Logger.class);

        getRoleService = new GetRoleService(rolePort, roleDtoMapper);

        // Manually inject the logger mock because it's a non-constructor private field (@EnableLogger)
        Field logField = GetRoleService.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(getRoleService, log);
    }

    @Test
    void shouldReturnRoleDetailsSuccessfullyWhenRoleExists() {
        // Arrange
        UUID roleId = UUID.randomUUID();
        EntityReference<UUID> auditRef = new EntityReference<>(
                null,
                new ResourceIdentifier("system", "system")
        );
        Role sampleRole = Role.reconstitute(
                roleId,
                "ADMIN",
                "Administrator Role",
                RoleStatus.ACTIVE,
                null,
                null,
                1L,
                1L,
                auditRef,
                LocalDateTime.now(),
                auditRef,
                LocalDateTime.now()
        );

        when(rolePort.findById(roleId)).thenReturn(Optional.of(sampleRole));

        // Act
        RoleDto result = getRoleService.getRoleById(roleId);

        // Assert
        assertNotNull(result);
        assertEquals(roleId, result.id());
        assertEquals("ADMIN", result.name());
        assertEquals("Administrator Role", result.description());
        assertEquals(RoleStatus.ACTIVE, result.status());

        verify(rolePort).findById(roleId);
        verify(log).info("Starting retrieval of role details for ID: {}", roleId);
        verify(log).info("Successfully retrieved role details for ID: {}", roleId);
    }

    @Test
    void shouldThrowRoleNotFoundExceptionWhenRoleDoesNotExist() {
        // Arrange
        UUID roleId = UUID.randomUUID();
        when(rolePort.findById(roleId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RoleNotFoundException.class, () -> getRoleService.getRoleById(roleId));

        verify(rolePort).findById(roleId);
        verify(log).info("Starting retrieval of role details for ID: {}", roleId);
        verify(log).error("Role details retrieval failed. Role not found for ID: {}", roleId);
    }
}
