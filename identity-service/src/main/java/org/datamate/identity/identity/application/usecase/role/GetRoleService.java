package org.datamate.identity.identity.application.usecase.role;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import lombok.RequiredArgsConstructor;
import org.datamate.identity.identity.application.dto.role.RoleDto;
import org.datamate.identity.identity.application.mapper.role.RoleDtoMapper;
import org.datamate.identity.identity.application.port.in.role.GetRoleUseCase;
import org.datamate.identity.identity.application.port.out.role.RolePersistencePort;
import org.datamate.identity.identity.domain.exception.role.RoleNotFoundException;
import org.datamate.identity.identity.domain.model.role.entity.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetRoleService implements GetRoleUseCase {

    @EnableLogger
    private Logger log;

    private final RolePersistencePort rolePort;
    private final RoleDtoMapper roleDtoMapper;

    @Override
    @Transactional(readOnly = true)
    public RoleDto getRoleById(UUID id) {
        log.info("Starting retrieval of role details for ID: {}", id);
        
        Role role = rolePort.findById(id).orElseThrow(() -> {
            log.error("Role details retrieval failed. Role not found for ID: {}", id);
            return new RoleNotFoundException();
        });

        log.info("Successfully retrieved role details for ID: {}", id);
        return roleDtoMapper.toDto(role);
    }
}
