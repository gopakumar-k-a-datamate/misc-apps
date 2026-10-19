package org.datamate.identity.identity.adapter.in.event;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.datamate.identity.identity.domain.event.user.UserCreatedEvent;
import org.datamate.identity.identity.domain.event.user.UserActivatedEvent;
import org.datamate.identity.identity.domain.event.user.UserDeactivatedEvent;
import org.datamate.identity.identity.domain.event.user.UserPasswordResetByAdminEvent;
import org.datamate.identity.identity.domain.event.user.UserPasswordChangedEvent;
import org.datamate.identity.identity.domain.event.user.UserInformationUpdatedEvent;
import org.datamate.identity.identity.domain.event.user.UserRolesUpdatedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener {

    @EnableLogger
    private Logger log;

    @ApplicationModuleListener
    public void onUserCreated(UserCreatedEvent event) {
        if (log != null) {
            log.info("Received UserCreatedEvent outbox event for user: {}", event.userName());
        }
    }

    @ApplicationModuleListener
    public void onUserActivated(UserActivatedEvent event) {
        if (log != null) {
            log.info("Received UserActivatedEvent outbox event for user ID: {}, activated by: {}", event.aggregateId(), event.activatedBy());
        }
    }

    @ApplicationModuleListener
    public void onUserPasswordResetByAdmin(UserPasswordResetByAdminEvent event) {
        if (log != null) {
            log.info("Received UserPasswordResetByAdminEvent outbox event for user ID: {}, reset by admin: {}", event.aggregateId(), event.resetBy());
        }
    }

    @ApplicationModuleListener
    public void onUserDeactivated(UserDeactivatedEvent event) {
        if (log != null) {
            log.info("Received UserDeactivatedEvent outbox event for user ID: {}, deactivated by: {}", event.aggregateId(), event.deactivatedBy());
        }
    }

    @ApplicationModuleListener
    public void onUserPasswordChanged(UserPasswordChangedEvent event) {
        if (log != null) {
            log.info("Received UserPasswordChangedEvent outbox event for user ID: {}, changed by: {}", event.aggregateId(), event.changedBy());
        }
    }

    @ApplicationModuleListener
    public void onUserInformationUpdated(UserInformationUpdatedEvent event) {
        if (log != null) {
            log.info("Received UserInformationUpdatedEvent outbox event for user ID: {}, updated by: {}", event.aggregateId(), event.updatedBy());
        }
    }

    @ApplicationModuleListener
    public void onUserRolesUpdated(UserRolesUpdatedEvent event) {
        if (log != null) {
            log.info("Received UserRolesUpdatedEvent outbox event for user ID: {}, assigned by: {}", event.aggregateId(), event.assignedBy());
        }
    }
}
