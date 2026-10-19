package org.datamate.identity.identity.domain.exception.auth;

import org.datamate.identity.identity.domain.exception.DomainException;

public class InvalidRefreshTokenException extends DomainException {
    public InvalidRefreshTokenException() {
        super("auth.invalidRefreshToken", "Invalid or expired refresh token.");
    }
}
