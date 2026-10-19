package org.datamate.identity.identity.application.port.in.role;

import com.datamate.bedrock.framework.common.ddd.datatype.EntityReference;
import java.util.UUID;

public interface ActivateRoleUseCase {
    void activateRole(UUID id, EntityReference<UUID> adminUserRef);
}
