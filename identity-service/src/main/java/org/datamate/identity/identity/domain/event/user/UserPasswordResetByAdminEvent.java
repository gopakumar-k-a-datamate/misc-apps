package org.datamate.identity.identity.domain.event.user;

import java.time.Instant;
import java.util.UUID;
import com.datamate.bedrock.framework.common.ddd.event.VersionedDomainEvent;

public record UserPasswordResetByAdminEvent(
        String eventId,
        UUID aggregateId,
        Long domainVersion,
        String schemaVersion,
        Instant occurredOn,
        String userName,
        String email,
        String phoneNumber,
        String firstName,
        String lastName,
        String resetBy
) implements VersionedDomainEvent<UUID> {
    public static final String SCHEMA_VERSION = "1.0";

    public UserPasswordResetByAdminEvent(
            UUID aggregateId, Long domainVersion, String userName, String email,
            String phoneNumber, String firstName, String lastName, String resetBy
    ) {
        this(
                UUID.randomUUID().toString(),
                aggregateId,
                domainVersion,
                SCHEMA_VERSION,
                Instant.now(),
                userName,
                email,
                phoneNumber,
                firstName,
                lastName,
                resetBy
        );
    }
}
