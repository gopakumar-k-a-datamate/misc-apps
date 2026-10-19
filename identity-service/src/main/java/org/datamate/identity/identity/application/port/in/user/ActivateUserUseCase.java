package org.datamate.identity.identity.application.port.in.user;

import java.util.UUID;

public interface ActivateUserUseCase {
    void activateUser(UUID id, String adminUsername);
}
