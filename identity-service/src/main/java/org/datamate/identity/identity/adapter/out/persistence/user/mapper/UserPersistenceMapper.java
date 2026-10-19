package org.datamate.identity.identity.adapter.out.persistence.user.mapper;

import org.datamate.identity.identity.adapter.out.persistence.role.entity.RoleJpaEntity;
import org.datamate.identity.identity.adapter.out.persistence.role.repository.SpringDataRoleRepository;
import org.datamate.identity.identity.adapter.out.persistence.user.entity.UserJpaEntity;
import org.datamate.identity.identity.domain.model.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.datamate.identity.identity.domain.exception.role.RoleNotFoundException;

@Component
public class UserPersistenceMapper {

    private final SpringDataRoleRepository roleRepository;

    public UserPersistenceMapper(SpringDataRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public User mapToDomain(UserJpaEntity entity) {
        if (entity == null) return null;
        return User.reconstitute(
                entity.getId(),
                entity.getUserName(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getPasswordHash(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getReferenceSystem(),
                entity.getReferenceValue(),
                entity.getStatus(),
                entity.getRoles() != null 
                        ? entity.getRoles().stream().map(r -> r.getId().toString()).collect(Collectors.toList()) 
                        : Collections.emptyList(),
                entity.isPasswordTemporary(),
                entity.getVersion(),
                entity.getDomainVersion(),
                entity.getCreatedBy(),
                entity.getCreatedDate(),
                entity.getLastModifiedBy(),
                entity.getLastModifiedDate()
        );
    }

    public UserJpaEntity mapToJpaEntity(User user) {
        if (user == null) return null;
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setUserName(user.getUserName());
        entity.setEmail(user.getEmail());
        entity.setPhoneNumber(user.getPhoneNumber());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setReferenceSystem(user.getReferenceSystem());
        entity.setReferenceValue(user.getReferenceValue());
        entity.setStatus(user.getStatus());
        entity.setPasswordTemporary(user.isPasswordTemporary());
        entity.setVersion(user.getVersion());
        entity.setDomainVersion(user.getDomainVersion());
        entity.setCreatedBy(user.getCreatedBy());
        entity.setCreatedDate(user.getCreatedDate());
        entity.setLastModifiedBy(user.getLastModifiedBy());
        entity.setLastModifiedDate(user.getLastModifiedDate());
        return entity;
    }

    public void updateJpaEntity(UserJpaEntity entity, User user) {
        if (user == null || entity == null) return;
        entity.setUserName(user.getUserName());
        entity.setEmail(user.getEmail());
        entity.setPhoneNumber(user.getPhoneNumber());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setReferenceSystem(user.getReferenceSystem());
        entity.setReferenceValue(user.getReferenceValue());
        entity.setStatus(user.getStatus());
        entity.setPasswordTemporary(user.isPasswordTemporary());
        entity.setVersion(user.getVersion());
        entity.setDomainVersion(user.getDomainVersion());
        entity.setCreatedBy(user.getCreatedBy());
        entity.setCreatedDate(user.getCreatedDate());
        entity.setLastModifiedBy(user.getLastModifiedBy());
        entity.setLastModifiedDate(user.getLastModifiedDate());

        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            List<java.util.UUID> roleUuids = new java.util.ArrayList<>();
            List<String> roleNames = new java.util.ArrayList<>();
            for (String r : user.getRoles()) {
                try {
                    roleUuids.add(java.util.UUID.fromString(r));
                } catch (IllegalArgumentException e) {
                    roleNames.add(r);
                }
            }
            List<RoleJpaEntity> roleEntitiesList = new java.util.ArrayList<>();
            if (!roleUuids.isEmpty()) {
                roleEntitiesList.addAll(roleRepository.findAllById(roleUuids));
            }
            if (!roleNames.isEmpty()) {
                roleEntitiesList.addAll(roleRepository.findAllByNameIn(roleNames));
            }
            if (roleEntitiesList.size() != user.getRoles().size()) {
                throw new RoleNotFoundException();
            }
            entity.setRoles(new HashSet<>(roleEntitiesList));
        } else {
            entity.getRoles().clear();
        }
    }
}
