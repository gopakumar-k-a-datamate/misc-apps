package org.datamate.identity.identity.domain.exception;

import org.datamate.identity.shared.exception.BaseException;

public class DomainException extends BaseException{

    protected DomainException(String errorCode) {
        super(errorCode);
    }

    protected DomainException(String errorCode, String defaultMessage) {
        super(errorCode, defaultMessage);
    }


    protected DomainException(String errorCode, String defaultMessage, Object... args) {
        super(errorCode, defaultMessage, args);
    }
}
