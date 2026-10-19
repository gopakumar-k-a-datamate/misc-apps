package org.datamate.identity.identity.application.port.in.role;

import org.datamate.identity.identity.application.dto.role.RoleDto;
import org.datamate.identity.identity.application.dto.role.RoleRequest;

public interface CreateRoleUseCase {
    RoleDto createRole(RoleRequest request);
}
