package org.datamate.identity.identity.application.port.in.role;

import com.datamate.bedrock.framework.common.ddd.datatype.EntityReference;
import java.util.UUID;

public interface DeactivateRoleUseCase {
    void deactivateRole(UUID id, EntityReference<UUID> adminUserRef);
}
