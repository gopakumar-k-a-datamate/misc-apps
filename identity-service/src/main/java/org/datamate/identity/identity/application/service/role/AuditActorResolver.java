package org.datamate.identity.identity.application.service.role;

import com.datamate.bedrock.framework.common.ddd.datatype.EntityReference;
import com.datamate.bedrock.framework.common.ddd.datatype.ResourceIdentifier;
import lombok.RequiredArgsConstructor;
import org.datamate.identity.identity.application.port.out.user.UserPersistencePort;
import org.datamate.identity.identity.domain.model.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditActorResolver {

    private final UserPersistencePort userPort;

    public EntityReference<UUID> resolve(String username) {
        if (username == null || username.isBlank()) {
            return new EntityReference<>(null, new ResourceIdentifier("system", "UNKNOWN"));
        }
        
        Optional<User> userOpt = Optional.empty();
        try {
            UUID userId = UUID.fromString(username);
            userOpt = userPort.findById(userId);
        } catch (IllegalArgumentException e) {
            userOpt = userPort.findByUserName(username);
        }

        return userOpt
                .map(user -> new EntityReference<>(user.getId(), new ResourceIdentifier("identity-service", user.getUserName())))
                .orElseGet(() -> new EntityReference<>(null, new ResourceIdentifier("system", username)));
    }
}
