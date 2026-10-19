package org.datamate.identity.identity.domain.event.role;

import java.time.Instant;
import java.util.UUID;
import com.datamate.bedrock.framework.common.ddd.datatype.EntityReference;
import com.datamate.bedrock.framework.common.ddd.event.VersionedDomainEvent;

public record RoleDeactivatedEvent(
        String eventId,
        UUID aggregateId,
        Long domainVersion,
        String schemaVersion,
        Instant occurredOn,
        String name,
        EntityReference<UUID> deactivatedBy
) implements VersionedDomainEvent<UUID> {
    public static final String SCHEMA_VERSION = "1.0";

    public RoleDeactivatedEvent(UUID aggregateId, Long domainVersion, String name, EntityReference<UUID> deactivatedBy) {
        this(
                UUID.randomUUID().toString(),
                aggregateId,
                domainVersion,
                SCHEMA_VERSION,
                Instant.now(),
                name,
                deactivatedBy
        );
    }
}
