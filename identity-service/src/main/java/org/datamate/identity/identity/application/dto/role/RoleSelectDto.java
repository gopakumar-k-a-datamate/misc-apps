package org.datamate.identity.identity.application.dto.role;

import java.util.UUID;

public record RoleSelectDto(
        UUID id,
        String name
) {}
