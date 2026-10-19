package org.datamate.identity.identity.application.query.user;

import org.datamate.identity.identity.domain.model.user.enums.UserStatus;

public record UserSearchCriteria(
    String search,
    String role,
    UserStatus status
) {}
