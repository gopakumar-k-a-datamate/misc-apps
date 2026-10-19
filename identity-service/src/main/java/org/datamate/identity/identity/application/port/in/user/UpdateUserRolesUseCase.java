package org.datamate.identity.identity.application.port.in.user;

import org.datamate.identity.identity.application.dto.user.UserDto;
import org.datamate.identity.identity.application.dto.user.UpdateUserRolesRequest;
import java.util.UUID;

public interface UpdateUserRolesUseCase {
    UserDto updateUserRoles(UUID userId, UpdateUserRolesRequest request, String adminUsername);
}
