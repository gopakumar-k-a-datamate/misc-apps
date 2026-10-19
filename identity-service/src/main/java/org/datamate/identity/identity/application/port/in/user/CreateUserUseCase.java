package org.datamate.identity.identity.application.port.in.user;

import org.datamate.identity.identity.application.dto.user.CreateUserRequest;
import org.datamate.identity.identity.application.dto.user.UserDto;

public interface CreateUserUseCase {
    UserDto createUser(CreateUserRequest request);
}
