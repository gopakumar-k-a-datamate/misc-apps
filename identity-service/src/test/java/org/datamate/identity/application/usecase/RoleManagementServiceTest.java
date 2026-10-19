package org.datamate.identity.application.usecase;

import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.datamate.identity.identity.application.port.out.role.RolePersistencePort;
import org.datamate.identity.identity.application.usecase.role.RoleManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.mockito.Mockito.*;

class RoleManagementServiceTest {

    private RolePersistencePort rolePort;
    private Logger log;
    private RoleManagementService roleManagementService;

    @BeforeEach
    void setUp() throws Exception {
        rolePort = mock(RolePersistencePort.class);
        log = mock(Logger.class);

        roleManagementService = new RoleManagementService(rolePort);

        // Manually inject the logger mock because it's a non-constructor private field (@EnableLogger)
        Field logField = RoleManagementService.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(roleManagementService, log);
    }

    @Test
    void shouldDeleteRoleSuccessfully() {
        UUID roleId = UUID.randomUUID();
        org.datamate.identity.identity.domain.model.role.entity.Role customRole = 
                org.datamate.identity.identity.domain.model.role.entity.Role.reconstitute(
                        roleId, "CUSTOM_ROLE", "Custom Desc", 
                        org.datamate.identity.identity.domain.model.role.enums.RoleStatus.ACTIVE, 
                        false, null, null, 1L, 1L, null, null, null, null
                );
        when(rolePort.findById(roleId)).thenReturn(java.util.Optional.of(customRole));

        roleManagementService.deleteRole(roleId);

        verify(rolePort).delete(roleId);
    }

    @Test
    void shouldThrowWhenDeletingSystemRole() {
        UUID roleId = UUID.randomUUID();
        org.datamate.identity.identity.domain.model.role.entity.Role systemRole = 
                org.datamate.identity.identity.domain.model.role.entity.Role.reconstitute(
                        roleId, "POLICY_ADMIN", "System Desc", 
                        org.datamate.identity.identity.domain.model.role.enums.RoleStatus.ACTIVE, 
                        true, null, null, 1L, 1L, null, null, null, null
                );
        when(rolePort.findById(roleId)).thenReturn(java.util.Optional.of(systemRole));

        org.junit.jupiter.api.Assertions.assertThrows(
                org.datamate.identity.identity.domain.exception.role.InvalidRoleDataException.class,
                () -> roleManagementService.deleteRole(roleId)
        );

        verify(rolePort, never()).delete(roleId);
    }
}
