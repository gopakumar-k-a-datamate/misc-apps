package org.datamate.identity.identity.domain.exception.user;

import org.datamate.identity.identity.domain.exception.DomainException;

public class UserInactiveException extends DomainException {

    public UserInactiveException() {
        super("user.inactive", "User account is inactive. Please contact administration.");
    }
}
