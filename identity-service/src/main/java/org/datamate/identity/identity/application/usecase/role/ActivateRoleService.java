package org.datamate.identity.identity.application.usecase.role;

import com.datamate.bedrock.framework.common.ddd.datatype.EntityReference;
import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import lombok.RequiredArgsConstructor;
import org.datamate.identity.identity.application.port.in.role.ActivateRoleUseCase;
import org.datamate.identity.identity.application.port.out.role.RolePersistencePort;
import org.datamate.identity.identity.domain.exception.role.RoleNotFoundException;
import org.datamate.identity.identity.domain.model.role.entity.Role;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivateRoleService implements ActivateRoleUseCase {

    @EnableLogger
    private Logger log;

    private final RolePersistencePort rolePort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void activateRole(UUID id, EntityReference<UUID> adminUserRef) {
        String adminVal = (adminUserRef != null && adminUserRef.identifier() != null) ? adminUserRef.identifier().value() : "unknown";
        log.info("Starting activation of role ID: {} by admin reference: {}", id, adminVal);

        Role role = rolePort.findById(id).orElseThrow(() -> {
            log.error("Role activation failed. Role not found for ID: {}", id);
            return new RoleNotFoundException();
        });

        Role activatedRole = role.activate(adminUserRef);
        rolePort.save(activatedRole);

        activatedRole.pullEvents().forEach(eventPublisher::publishEvent);

        log.info("Successfully activated role ID: {} by admin reference: {}", id, adminVal);
    }
}
