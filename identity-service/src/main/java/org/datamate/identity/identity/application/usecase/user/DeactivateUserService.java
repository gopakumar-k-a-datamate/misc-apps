package org.datamate.identity.identity.application.usecase.user;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import lombok.RequiredArgsConstructor;
import org.datamate.identity.identity.application.port.in.user.DeactivateUserUseCase;
import org.datamate.identity.identity.application.port.out.user.UserPersistencePort;
import org.datamate.identity.identity.domain.exception.user.UserNotFoundException;
import org.datamate.identity.identity.domain.model.user.entity.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.datamate.identity.identity.domain.constant.IdentityConstants;
import org.datamate.identity.identity.domain.exception.user.InvalidUserDataException;
import org.datamate.identity.identity.domain.model.user.enums.UserStatus;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeactivateUserService implements DeactivateUserUseCase {

    @EnableLogger
    private Logger log;

    private final UserPersistencePort userPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void deactivateUser(UUID id, String adminUsername) {
        log.info("Starting deactivation of user ID: {} by admin: {}", id, adminUsername);

        User user = userPort.findById(id).orElseThrow(() -> {
            log.error("User deactivation failed. User not found for ID: {}", id);
            return new UserNotFoundException();
        });

        boolean hasPolicyAdmin = user.getRoles() != null && (
                user.getRoles().contains(IdentityConstants.POLICY_ADMIN_ROLE_ID_STRING) ||
                user.getRoles().contains(IdentityConstants.ROLE_POLICY_ADMIN)
        );
        if (hasPolicyAdmin && user.getStatus() == UserStatus.ACTIVE) {
            long otherActiveAdmins = userPort.countActiveUsersWithRoleExcept(IdentityConstants.ROLE_POLICY_ADMIN, id)
                    + userPort.countActiveUsersWithRoleExcept(IdentityConstants.POLICY_ADMIN_ROLE_ID_STRING, id);
            if (otherActiveAdmins == 0) {
                log.warn("Attempted to deactivate the last active POLICY_ADMIN (user ID: {})", id);
                throw new InvalidUserDataException("user.validation.last.policy.admin", "Cannot deactivate user: this is the last active POLICY_ADMIN account in the system.");
            }
        }

        User deactivatedUser = user.deactivate(adminUsername);
        userPort.save(deactivatedUser);

        deactivatedUser.pullEvents().forEach(eventPublisher::publishEvent);

        log.info("Successfully deactivated user ID: {} by admin: {}", id, adminUsername);
    }
}
