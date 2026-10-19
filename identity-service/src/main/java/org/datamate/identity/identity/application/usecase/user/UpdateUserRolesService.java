package org.datamate.identity.identity.application.usecase.user;

import lombok.RequiredArgsConstructor;
import org.datamate.identity.identity.application.dto.user.UpdateUserRolesRequest;
import org.datamate.identity.identity.application.dto.user.UserDto;
import org.datamate.identity.identity.application.mapper.user.UserDtoMapper;
import org.datamate.identity.identity.application.port.in.user.UpdateUserRolesUseCase;
import org.datamate.identity.identity.application.port.out.role.RolePersistencePort;
import org.datamate.identity.identity.application.port.out.user.UserPersistencePort;
import org.datamate.identity.identity.domain.constant.IdentityConstants;
import org.datamate.identity.identity.domain.exception.role.RoleNotFoundException;
import org.datamate.identity.identity.domain.exception.user.InvalidRoleAssignmentException;
import org.datamate.identity.identity.domain.exception.user.UserNotFoundException;
import org.datamate.identity.identity.domain.model.role.entity.Role;
import org.datamate.identity.identity.domain.model.user.entity.User;
import org.datamate.identity.identity.domain.model.role.enums.RoleStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateUserRolesService implements UpdateUserRolesUseCase {

    private final UserPersistencePort userPort;
    private final RolePersistencePort rolePort;
    private final UserDtoMapper userDtoMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UserDto updateUserRoles(UUID userId, UpdateUserRolesRequest request, String adminUsername) {
        User user = userPort.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // Validate that all roles exist and are active
        List<Role> allRoles = rolePort.findAll();
        List<String> resolvedRoleIds = new java.util.ArrayList<>();

        for (String roleIdentifier : request.roles()) {
            Role role = allRoles.stream()
                    .filter(r -> r.getName().equalsIgnoreCase(roleIdentifier) || r.getId().toString().equals(roleIdentifier))
                    .findFirst()
                    .orElseThrow(RoleNotFoundException::new);

            if (role.getStatus() != RoleStatus.ACTIVE) {
                throw new InvalidRoleAssignmentException(role.getName());
            }

            resolvedRoleIds.add(role.getId().toString());

            boolean isPolicyAdminRole = role.getId().toString().equals(IdentityConstants.POLICY_ADMIN_ROLE_ID_STRING) ||
                                        IdentityConstants.ROLE_POLICY_ADMIN.equalsIgnoreCase(role.getName());

            if (isPolicyAdminRole) {
                boolean isCallerPolicyAdmin = adminUsername != null && (
                        adminUsername.equalsIgnoreCase(IdentityConstants.ADMIN_USERNAME) ||
                        userPort.findByUserNameOrEmail(adminUsername, adminUsername)
                                .map(u -> u.getRoles() != null && (
                                        u.getRoles().contains(IdentityConstants.POLICY_ADMIN_ROLE_ID_STRING) ||
                                        u.getRoles().contains(IdentityConstants.ROLE_POLICY_ADMIN)
                                ))
                                .orElse(false)
                );
                if (!isCallerPolicyAdmin) {
                    throw new InvalidRoleAssignmentException("Access Denied: Only a POLICY_ADMIN can assign the POLICY_ADMIN role.", role.getName());
                }
            }
        }

        boolean currentlyHasPolicyAdmin = user.getRoles() != null && (
                user.getRoles().contains(IdentityConstants.POLICY_ADMIN_ROLE_ID_STRING) ||
                user.getRoles().contains(IdentityConstants.ROLE_POLICY_ADMIN)
        );
        boolean willHavePolicyAdmin = resolvedRoleIds.contains(IdentityConstants.POLICY_ADMIN_ROLE_ID_STRING);

        if (currentlyHasPolicyAdmin && !willHavePolicyAdmin) {
            long otherActivePolicyAdmins = userPort.countActiveUsersWithRoleExcept(IdentityConstants.ROLE_POLICY_ADMIN, userId)
                    + userPort.countActiveUsersWithRoleExcept(IdentityConstants.POLICY_ADMIN_ROLE_ID_STRING, userId);
            if (otherActivePolicyAdmins == 0) {
                throw new InvalidRoleAssignmentException("Cannot remove POLICY_ADMIN: at least one active POLICY_ADMIN must exist in the system.", IdentityConstants.ROLE_POLICY_ADMIN);
            }
        }

        User updatedUser = user.assignRoles(resolvedRoleIds, adminUsername);
        User savedUser = userPort.save(updatedUser);

        updatedUser.pullEvents().forEach(eventPublisher::publishEvent);

        return userDtoMapper.toDto(savedUser);
    }
}
