package org.datamate.identity.identity.domain.event.role;

import java.time.Instant;
import java.util.UUID;
import com.datamate.bedrock.framework.common.ddd.datatype.EntityReference;
import com.datamate.bedrock.framework.common.ddd.event.VersionedDomainEvent;

public record RoleUpdatedEvent(
        String eventId,
        UUID aggregateId,
        Long domainVersion,
        String schemaVersion,
        Instant occurredOn,
        String name,
        String description,
        EntityReference<UUID> updatedBy
) implements VersionedDomainEvent<UUID> {
    public static final String SCHEMA_VERSION = "1.0";

    public RoleUpdatedEvent(UUID aggregateId, Long domainVersion, String name, String description, EntityReference<UUID> updatedBy) {
        this(UUID.randomUUID().toString(), aggregateId, domainVersion, SCHEMA_VERSION, Instant.now(), name, description, updatedBy);
    }
}
