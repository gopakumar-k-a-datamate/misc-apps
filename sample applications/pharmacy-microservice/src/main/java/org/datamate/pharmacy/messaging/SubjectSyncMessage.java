package org.datamate.pharmacy.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Anti-Corruption Layer (ACL) DTO for the subject sync wire contract.
 *
 * <p>This record represents the Pharmacy Service's local view of the
 * {@code auth.subject.sync} message. It is intentionally separate from
 * {@code authz-core}'s internal {@code AuthzSubjectSyncEvent}.</p>
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} ensures forward compatibility —
 * if the Identity Service adds new optional fields, deserialization will not fail.</p>
 *
 * <p>The wire contract is defined in: {@code docs/contracts/subject-sync-wire-contract.md}</p>
 *
 * @param subjectType   {@code "USER"} or {@code "ROLE"}
 * @param subjectId     IdP-issued UUID identifier
 * @param subjectName   Internal identifier (userName for users, role code for roles)
 * @param displayName   Human-readable label ("John Doe" for users, role name for roles)
 * @param email         Email address. {@code null} for ROLE subjects.
 * @param description   Role description. {@code null} for USER subjects.
 * @param status        Identity Service status: {@code "ACTIVE"} or {@code "INACTIVE"}
 * @param version       Monotonically increasing domain version; used for idempotency
 * @param deleted       {@code true} = deactivated; {@code false} = active
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubjectSyncMessage(
        String subjectType,
        String subjectId,
        String subjectName,
        String displayName,
        String email,
        String description,
        String status,
        long version,
        boolean deleted
) {}
