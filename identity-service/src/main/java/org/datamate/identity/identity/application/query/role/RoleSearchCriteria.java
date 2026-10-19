package org.datamate.identity.identity.application.query.role;

import org.datamate.identity.identity.domain.model.role.enums.RoleStatus;

public record RoleSearchCriteria(
    String search,
    RoleStatus status
) {}
