package org.datamate.identity.identity.application.dto.role;

import org.datamate.identity.identity.domain.model.role.enums.RoleStatus;
import java.util.UUID;

public record RoleDto(
        UUID id,
        String name,
        String description,
        RoleStatus status,
        boolean isSystem
) {}

