package org.datamate.identity.identity.application.port.in.user;

import org.datamate.identity.identity.application.dto.user.UserDto;
import java.util.UUID;

public interface GetUserUseCase {
    UserDto getUserById(UUID id);
}
