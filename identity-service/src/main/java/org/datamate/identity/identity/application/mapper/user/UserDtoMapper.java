package org.datamate.identity.identity.application.mapper.user;

import lombok.RequiredArgsConstructor;
import org.datamate.identity.identity.application.dto.user.UserDto;
import org.datamate.identity.identity.application.dto.user.UserResponseDto;
import org.datamate.identity.identity.domain.model.user.entity.User;
import com.datamate.bedrock.framework.common.pagination.Paged;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDtoMapper {

    public UserDto toDto(User entity) {
        if (entity == null) return null;
        return new UserDto(
                entity.getId(),
                entity.getUserName(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getReferenceSystem(),
                entity.getReferenceValue(),
                entity.getCreatedBy(),
                entity.getCreatedDate(),
                entity.getStatus(),
                entity.getRoles(),
                entity.isPasswordTemporary()
        );
    }

    public UserResponseDto toResponseDto(User entity) {
        if (entity == null) return null;
        return new UserResponseDto(
                entity.getId(),
                entity.getUserName(),
                entity.getEmail(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getStatus(),
                entity.getRoles()
        );
    }

    public Paged<UserResponseDto> toPaged(Paged<User> paged) {
        if (paged == null) return null;
        return paged.map(this::toResponseDto);
    }
}
