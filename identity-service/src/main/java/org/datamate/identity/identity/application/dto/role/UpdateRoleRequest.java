package org.datamate.identity.identity.application.dto.role;

import jakarta.validation.constraints.NotBlank;

public record UpdateRoleRequest(
        @NotBlank(message = "{role.validation.name.required}")
        String name,

        String description
) {}
