package org.datamate.identity.identity.application.port.in.role;

import java.util.UUID;

public interface RoleManagementUseCase {
    void deleteRole(UUID id);
}
