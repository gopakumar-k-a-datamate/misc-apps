package org.datamate.identity.identity.adapter.in.event;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.datamate.identity.identity.domain.event.role.RoleCreatedEvent;
import org.datamate.identity.identity.domain.event.role.RoleUpdatedEvent;
import org.datamate.identity.identity.domain.event.role.RoleActivatedEvent;
import org.datamate.identity.identity.domain.event.role.RoleDeactivatedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class RoleEventListener {

    @EnableLogger
    private Logger log;

    @ApplicationModuleListener
    public void onRoleCreated(RoleCreatedEvent event) {
        if (log != null) {
            log.info("Received RoleCreatedEvent outbox event for role: {}", event.name());
        }
    }

    @ApplicationModuleListener
    public void onRoleUpdated(RoleUpdatedEvent event) {
        if (log != null) {
            log.info("Received RoleUpdatedEvent outbox event for role: {}", event.name());
        }
    }

    @ApplicationModuleListener
    public void onRoleActivated(RoleActivatedEvent event) {
        if (log != null) {
            log.info("Received RoleActivatedEvent outbox event for role: {}", event.name());
        }
    }

    @ApplicationModuleListener
    public void onRoleDeactivated(RoleDeactivatedEvent event) {
        if (log != null) {
            log.info("Received RoleDeactivatedEvent outbox event for role: {}", event.name());
        }
    }
}
