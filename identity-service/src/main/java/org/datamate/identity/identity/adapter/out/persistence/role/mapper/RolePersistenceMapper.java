package org.datamate.identity.identity.adapter.out.persistence.role.mapper;

import org.datamate.identity.identity.adapter.out.persistence.role.entity.RoleJpaEntity;
import org.datamate.identity.identity.domain.model.role.entity.Role;
import com.datamate.bedrock.framework.common.ddd.datatype.EntityReference;
import com.datamate.bedrock.framework.common.ddd.datatype.ResourceIdentifier;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RolePersistenceMapper {

    public Role mapToDomain(RoleJpaEntity entity) {
        if (entity == null) return null;

        return Role.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStatus(),
                entity.isSystem(),
                entity.getReferenceSystem(),
                entity.getReferenceValue(),
                entity.getVersion(),
                entity.getDomainVersion(),
                toReference(entity.getCreatedById(), entity.getCreatedBySystem(), entity.getCreatedByValue()),
                entity.getCreatedDate(),
                toReference(entity.getLastModifiedById(), entity.getLastModifiedBySystem(), entity.getLastModifiedByValue()),
                entity.getLastModifiedDate()
        );
    }

    public RoleJpaEntity mapToJpaEntity(Role role) {
        if (role == null) return null;
        RoleJpaEntity entity = new RoleJpaEntity();
        entity.setId(role.getId());
        entity.setName(role.getName());
        entity.setDescription(role.getDescription());
        entity.setStatus(role.getStatus());
        entity.setSystem(role.isSystem());
        entity.setReferenceSystem(role.getReferenceSystem());
        entity.setReferenceValue(role.getReferenceValue());
        entity.setVersion(role.getVersion());
        entity.setDomainVersion(role.getDomainVersion());
        
        populateCreatedBy(entity, role.getCreatedBy());
        entity.setCreatedDate(role.getCreatedDate());
        
        populateLastModifiedBy(entity, role.getLastModifiedBy());
        entity.setLastModifiedDate(role.getLastModifiedDate());
        
        return entity;
    }

    private EntityReference<UUID> toReference(UUID id, String system, String value) {
        if (id == null && value == null) {
            return null;
        }
        return new EntityReference<>(id, new ResourceIdentifier(system, value));
    }

    private void populateCreatedBy(RoleJpaEntity entity, EntityReference<UUID> createdBy) {
        if (createdBy != null) {
            entity.setCreatedById(createdBy.id());
            if (createdBy.identifier() != null) {
                entity.setCreatedBySystem(createdBy.identifier().system());
                entity.setCreatedByValue(createdBy.identifier().value());
            }
        }
    }

    private void populateLastModifiedBy(RoleJpaEntity entity, EntityReference<UUID> lastModifiedBy) {
        if (lastModifiedBy != null) {
            entity.setLastModifiedById(lastModifiedBy.id());
            if (lastModifiedBy.identifier() != null) {
                entity.setLastModifiedBySystem(lastModifiedBy.identifier().system());
                entity.setLastModifiedByValue(lastModifiedBy.identifier().value());
            }
        }
    }
}
