package org.datamate.identity.identity.domain.model.role.entity;

import lombok.Getter;
import org.datamate.identity.identity.domain.model.role.enums.RoleStatus;
import org.datamate.identity.identity.domain.exception.role.InvalidRoleDataException;
import com.datamate.bedrock.framework.common.ddd.domain.AggregateRoot;
import com.datamate.bedrock.framework.common.ddd.datatype.EntityReference;

import java.time.LocalDateTime;
import java.util.UUID;

import org.datamate.identity.identity.domain.event.role.RoleCreatedEvent;
import org.datamate.identity.identity.domain.event.role.RoleUpdatedEvent;
import org.datamate.identity.identity.domain.event.role.RoleActivatedEvent;
import org.datamate.identity.identity.domain.event.role.RoleDeactivatedEvent;

@Getter
public class Role extends AggregateRoot {
    private final UUID id;
    private final String name;
    private final String description;
    private final RoleStatus status;
    private final boolean isSystem;
    private final String referenceSystem;
    private final String referenceValue;
    private final Long version;
    private final EntityReference<UUID> createdBy;
    private final LocalDateTime createdDate;
    private final EntityReference<UUID> lastModifiedBy;
    private final LocalDateTime lastModifiedDate;

    private Role(
            UUID id,
            String name,
            String description,
            RoleStatus status,
            boolean isSystem,
            String referenceSystem,
            String referenceValue,
            Long version,
            Long domainVersion,
            EntityReference<UUID> createdBy,
            LocalDateTime createdDate,
            EntityReference<UUID> lastModifiedBy,
            LocalDateTime lastModifiedDate
    ) {
        super(domainVersion);
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.isSystem = isSystem;
        this.referenceSystem = referenceSystem;
        this.referenceValue = referenceValue;
        this.version = version;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.lastModifiedBy = lastModifiedBy;
        this.lastModifiedDate = lastModifiedDate;
    }

    public static Role create(
            String name,
            String description,
            EntityReference<UUID> createdBy
    ) {
        validateState(name, RoleStatus.INACTIVE, createdBy);

        return new Role(
                null,
                name,
                description,
                RoleStatus.INACTIVE,
                false,
                null,
                null,
                null,
                0L,
                createdBy,
                LocalDateTime.now(),
                createdBy,
                LocalDateTime.now()
        );
    }

    public static Role reconstitute(
            UUID id,
            String name,
            String description,
            RoleStatus status,
            boolean isSystem,
            String referenceSystem,
            String referenceValue,
            Long version,
            Long domainVersion,
            EntityReference<UUID> createdBy,
            LocalDateTime createdDate,
            EntityReference<UUID> lastModifiedBy,
            LocalDateTime lastModifiedDate
    ) {
        return new Role(
                id,
                name,
                description,
                status,
                isSystem,
                referenceSystem,
                referenceValue,
                version,
                domainVersion,
                createdBy,
                createdDate,
                lastModifiedBy,
                lastModifiedDate
        );
    }

    public static Role reconstitute(
            UUID id,
            String name,
            String description,
            RoleStatus status,
            String referenceSystem,
            String referenceValue,
            Long version,
            Long domainVersion,
            EntityReference<UUID> createdBy,
            LocalDateTime createdDate,
            EntityReference<UUID> lastModifiedBy,
            LocalDateTime lastModifiedDate
    ) {
        return reconstitute(
                id,
                name,
                description,
                status,
                false,
                referenceSystem,
                referenceValue,
                version,
                domainVersion,
                createdBy,
                createdDate,
                lastModifiedBy,
                lastModifiedDate
        );
    }

    private static void validateState(String name, RoleStatus status, EntityReference<UUID> createdBy) {
        if (name == null || name.isBlank()) {
            throw new InvalidRoleDataException("role.validation.name.required", "Role name is mandatory.");
        }
        if (status == null) {
            throw new InvalidRoleDataException("role.validation.status.required", "Role status is mandatory.");
        }
        if (createdBy == null) {
            throw new InvalidRoleDataException("role.validation.createdBy.required", "Created by reference is required.");
        }
    }

    public Role publishCreationEvent() {
        this.registerEvent(new RoleCreatedEvent(
                this.id,
                this.getDomainVersion(),
                this.name,
                this.description,
                this.status,
                this.createdBy
        ));
        return this;
    }

    public Role updateInformation(String name, String description, EntityReference<UUID> lastModifiedBy) {
        validateUpdate(name, lastModifiedBy);

        if (this.isSystem && !this.name.equalsIgnoreCase(name)) {
            throw new InvalidRoleDataException("role.validation.system.role", "Built-in system role '" + this.name + "' cannot be renamed.");
        }

        Role updatedRole = new Role(
                this.id,
                name,
                description,
                this.status,
                this.isSystem,
                this.referenceSystem,
                this.referenceValue,
                this.version,
                this.getDomainVersion(),
                this.createdBy,
                this.createdDate,
                lastModifiedBy,
                LocalDateTime.now()
        );

        updatedRole.registerEvent(new RoleUpdatedEvent(
                this.id,
                updatedRole.getDomainVersion() + 1,
                name,
                description,
                lastModifiedBy
        ));

        return updatedRole;
    }

    public Role activate(EntityReference<UUID> updatedBy) {
        if (this.status == RoleStatus.ACTIVE) {
            throw new InvalidRoleDataException("role.validation.already.active", "Role is already active.");
        }
        Role updatedRole = new Role(
                this.id,
                this.name,
                this.description,
                RoleStatus.ACTIVE,
                this.isSystem,
                this.referenceSystem,
                this.referenceValue,
                this.version,
                this.getDomainVersion(),
                this.createdBy,
                this.createdDate,
                updatedBy,
                LocalDateTime.now()
        );

        updatedRole.registerEvent(new RoleActivatedEvent(
                this.id,
                updatedRole.getDomainVersion() + 1,
                this.name,
                updatedBy
        ));

        return updatedRole;
    }

    public Role deactivate(EntityReference<UUID> updatedBy) {
        if (this.isSystem) {
            throw new InvalidRoleDataException("role.validation.system.role", "Built-in system role '" + this.name + "' cannot be deactivated.");
        }
        if (this.status == RoleStatus.INACTIVE) {
            throw new InvalidRoleDataException("role.validation.already.inactive", "Role is already inactive.");
        }
        Role updatedRole = new Role(
                this.id,
                this.name,
                this.description,
                RoleStatus.INACTIVE,
                this.isSystem,
                this.referenceSystem,
                this.referenceValue,
                this.version,
                this.getDomainVersion(),
                this.createdBy,
                this.createdDate,
                updatedBy,
                LocalDateTime.now()
        );

        updatedRole.registerEvent(new RoleDeactivatedEvent(
                this.id,
                updatedRole.getDomainVersion() + 1,
                this.name,
                updatedBy
        ));

        return updatedRole;
    }

    private void validateUpdate(String name, EntityReference<UUID> lastModifiedBy) {
        if (name == null || name.isBlank()) {
            throw new InvalidRoleDataException("role.validation.name.required", "Role name is mandatory.");
        }
        if (lastModifiedBy == null) {
            throw new InvalidRoleDataException("role.validation.updatedBy.required", "Updated by reference is required.");
        }
    }
}
