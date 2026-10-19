package org.datamate.identity.identity.application.usecase.role;

import com.datamate.bedrock.framework.common.ddd.datatype.EntityReference;
import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import lombok.RequiredArgsConstructor;
import org.datamate.identity.identity.application.port.in.role.DeactivateRoleUseCase;
import org.datamate.identity.identity.application.port.out.role.RolePersistencePort;
import org.datamate.identity.identity.domain.exception.role.RoleNotFoundException;
import org.datamate.identity.identity.domain.model.role.entity.Role;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeactivateRoleService implements DeactivateRoleUseCase {

    @EnableLogger
    private Logger log;

    private final RolePersistencePort rolePort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void deactivateRole(UUID id, EntityReference<UUID> adminUserRef) {
        String adminVal = (adminUserRef != null && adminUserRef.identifier() != null) ? adminUserRef.identifier().value() : "unknown";
        log.info("Starting deactivation of role ID: {} by admin reference: {}", id, adminVal);

        Role role = rolePort.findById(id).orElseThrow(() -> {
            log.error("Role deactivation failed. Role not found for ID: {}", id);
            return new RoleNotFoundException();
        });

        Role deactivatedRole = role.deactivate(adminUserRef);
        rolePort.save(deactivatedRole);

        deactivatedRole.pullEvents().forEach(eventPublisher::publishEvent);

        log.info("Successfully deactivated role ID: {} by admin reference: {}", id, adminVal);
    }
}
