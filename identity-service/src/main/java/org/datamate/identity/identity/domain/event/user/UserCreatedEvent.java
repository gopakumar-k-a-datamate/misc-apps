package org.datamate.identity.identity.domain.event.user;

import java.time.Instant;
import java.util.UUID;
import org.datamate.identity.identity.domain.model.user.enums.UserStatus;
import com.datamate.bedrock.framework.common.ddd.event.VersionedDomainEvent;

import java.util.List;

public record UserCreatedEvent(
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
        UserStatus status,
        List<String> roles,
        String createdBy
) implements VersionedDomainEvent<UUID> {
    public static final String SCHEMA_VERSION = "1.0";

    public UserCreatedEvent(
            UUID aggregateId, Long domainVersion, String userName, String email,
            String phoneNumber, String firstName, String lastName, UserStatus status,
            List<String> roles, String createdBy
    ) {
        this(
                UUID.randomUUID().toString(), aggregateId, domainVersion, SCHEMA_VERSION, Instant.now(),
                userName, email, phoneNumber, firstName, lastName, status, roles, createdBy
        );
    }
}
