package org.datamate.identity.identity.application.usecase.role;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import lombok.RequiredArgsConstructor;
import org.datamate.identity.identity.application.port.in.role.RoleManagementUseCase;
import org.datamate.identity.identity.application.port.out.role.RolePersistencePort;
import org.springframework.stereotype.Service;

import org.datamate.identity.identity.domain.model.role.entity.Role;
import org.datamate.identity.identity.domain.exception.role.RoleNotFoundException;
import org.datamate.identity.identity.domain.exception.role.InvalidRoleDataException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleManagementService implements RoleManagementUseCase {

    @EnableLogger
    private Logger log;

    private final RolePersistencePort rolePort;

    @Override
    public void deleteRole(UUID id) {
        log.info("Deleting role with id {}", id);
        Role role = rolePort.findById(id)
                .orElseThrow(RoleNotFoundException::new);

        if (role.isSystem()) {
            throw new InvalidRoleDataException("role.validation.system.role", "Built-in system role '" + role.getName() + "' cannot be deleted.");
        }

        rolePort.delete(id);
    }
}
