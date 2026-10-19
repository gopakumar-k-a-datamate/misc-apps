package org.datamate.identity.identity.domain.exception.auth;

import org.datamate.identity.identity.domain.exception.DomainException;

public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("user.invalidCredentials", "Invalid username or password.");
    }
}
