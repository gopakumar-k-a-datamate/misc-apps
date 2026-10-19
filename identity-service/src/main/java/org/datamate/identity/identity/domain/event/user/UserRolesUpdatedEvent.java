package org.datamate.identity.identity.domain.event.user;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.datamate.bedrock.framework.common.ddd.event.VersionedDomainEvent;

public record UserRolesUpdatedEvent(
        String eventId,
        UUID aggregateId,
        Long domainVersion,
        String schemaVersion,
        Instant occurredOn,
        List<String> roles,
        String assignedBy
) implements VersionedDomainEvent<UUID> {
    public static final String SCHEMA_VERSION = "1.0";

    public UserRolesUpdatedEvent(UUID aggregateId, Long domainVersion, List<String> roles, String assignedBy) {
        this(
                UUID.randomUUID().toString(),
                aggregateId,
                domainVersion,
                SCHEMA_VERSION,
                Instant.now(),
                roles,
                assignedBy
        );
    }
}
