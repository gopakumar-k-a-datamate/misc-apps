package org.datamate.identity.identity.domain.exception.role;

import org.datamate.identity.identity.domain.exception.DomainException;

public class RoleNotFoundException extends DomainException {

    public RoleNotFoundException() {
        super("role.notFound", "Role not found.");
    }
}
