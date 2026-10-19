package org.datamate.identity.identity.domain.exception.user;

import org.datamate.identity.identity.domain.exception.DomainException;

public class InvalidUserDataException extends DomainException {

    public InvalidUserDataException(String errorCode, String defaultMessage) {
        super(errorCode, defaultMessage);
    }
}
