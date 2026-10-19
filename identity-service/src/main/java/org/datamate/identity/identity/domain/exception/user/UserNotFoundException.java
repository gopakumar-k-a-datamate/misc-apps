package org.datamate.identity.identity.domain.exception.user;

import org.datamate.identity.identity.domain.exception.DomainException;

public class UserNotFoundException extends DomainException {

    public UserNotFoundException() {
        super("user.notFound", "User not found.");
    }
}
