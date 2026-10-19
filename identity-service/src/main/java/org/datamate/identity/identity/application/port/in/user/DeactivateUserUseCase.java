package org.datamate.identity.identity.application.port.in.user;

import java.util.UUID;

public interface DeactivateUserUseCase {
    void deactivateUser(UUID id, String adminUsername);
}
