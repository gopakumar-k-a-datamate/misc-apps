package org.datamate.identity.identity.application.usecase.role;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import lombok.RequiredArgsConstructor;
import org.datamate.identity.identity.application.dto.role.RoleSelectDto;
import org.datamate.identity.identity.application.mapper.role.RoleDtoMapper;
import org.datamate.identity.identity.application.port.in.role.SelectRolesUseCase;
import org.datamate.identity.identity.application.port.out.role.RolePersistencePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SelectRolesService implements SelectRolesUseCase {

    @EnableLogger
    private Logger log;

    private final RolePersistencePort rolePort;
    private final RoleDtoMapper roleDtoMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RoleSelectDto> selectRoles(String search) {
        log.info("Selecting active roles with search query: '{}'", search);
        return rolePort.findActiveRoles(search)
                .stream()
                .map(roleDtoMapper::toSelectDto)
                .collect(Collectors.toList());
    }
}
