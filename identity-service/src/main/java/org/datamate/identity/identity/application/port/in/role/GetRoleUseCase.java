package org.datamate.identity.identity.application.port.in.role;

import org.datamate.identity.identity.application.dto.role.RoleDto;
import java.util.UUID;

public interface GetRoleUseCase {
    RoleDto getRoleById(UUID id);
}
