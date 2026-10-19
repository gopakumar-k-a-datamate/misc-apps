package org.datamate.identity.identity.domain.event.role;

import java.time.Instant;
import java.util.UUID;
import org.datamate.identity.identity.domain.model.role.enums.RoleStatus;
import com.datamate.bedrock.framework.common.ddd.event.VersionedDomainEvent;
import com.datamate.bedrock.framework.common.ddd.datatype.EntityReference;

public record RoleCreatedEvent(
        String eventId,
        UUID aggregateId,
        Long domainVersion,
        String schemaVersion,
        Instant occurredOn,

        String name,
        String description,
        RoleStatus status,
        EntityReference<UUID> createdBy
) implements VersionedDomainEvent<UUID> {
    public static final String SCHEMA_VERSION = "1.0";

    public RoleCreatedEvent(
            UUID aggregateId, Long domainVersion, String name, String description,
            RoleStatus status, EntityReference<UUID> createdBy
    ) {
        this(
                UUID.randomUUID().toString(), aggregateId, domainVersion, SCHEMA_VERSION, Instant.now(),
                name, description, status, createdBy
        );
    }
}

