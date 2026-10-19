package org.datamate.identity.identity.application.mapper.role;

import org.datamate.identity.identity.application.dto.role.RoleDto;
import org.datamate.identity.identity.application.dto.role.RoleSelectDto;
import org.datamate.identity.identity.domain.model.role.entity.Role;

import com.datamate.bedrock.framework.common.pagination.Paged;
import org.springframework.stereotype.Component;

@Component
public class RoleDtoMapper {

    public RoleDto toDto(Role role) {
        if (role == null) return null;
        return new RoleDto(role.getId(), role.getName(), role.getDescription(), role.getStatus(), role.isSystem());
    }

    public RoleSelectDto toSelectDto(Role role) {
        if (role == null) return null;
        return new RoleSelectDto(role.getId(), role.getName());
    }

    public Paged<RoleDto> toPaged(Paged<Role> paged) {
        if (paged == null) return null;
        return paged.map(this::toDto);
    }
}
