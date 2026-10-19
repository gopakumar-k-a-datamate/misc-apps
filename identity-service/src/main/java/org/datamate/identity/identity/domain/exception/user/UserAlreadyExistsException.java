package org.datamate.identity.identity.domain.exception.user;

import org.datamate.identity.identity.domain.exception.DomainException;

public class UserAlreadyExistsException extends DomainException {

    public UserAlreadyExistsException() {
        super("user.alreadyExists", "A user with this identity already exists.");
    }
}
