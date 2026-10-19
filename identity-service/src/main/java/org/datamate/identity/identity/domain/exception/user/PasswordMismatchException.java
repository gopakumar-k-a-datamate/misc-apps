package org.datamate.identity.identity.domain.exception.user;

import org.datamate.identity.identity.domain.exception.DomainException;

public class PasswordMismatchException extends DomainException {

    public PasswordMismatchException() {
        super("user.passwordMismatch", "The provided password does not match the current password.");
    }
}
