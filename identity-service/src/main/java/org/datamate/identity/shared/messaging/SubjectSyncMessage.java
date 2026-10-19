package org.datamate.identity.shared.messaging;

/**
 * Internal message record used by the Identity Service to broadcast
 * subject lifecycle changes via RabbitMQ.
 *
 * <p>This is the Identity Service's own representation of the wire contract
 * defined in {@code docs/contracts/subject-sync-wire-contract.md}.
 * It is intentionally <strong>not</strong> imported from any external library.</p>
 *
 * <p><strong>Unified Subject Model:</strong> Fields not applicable to a subject type
 * must be set to {@code null} (e.g. {@code email} for a ROLE subject).</p>
 *
 * @param subjectType   {@code "USER"} or {@code "ROLE"}
 * @param subjectId     IdP-issued UUID identifier
 * @param subjectName   Internal identifier: userName for users, role code for roles
 * @param displayName   Human-readable label: "John Doe" for users, role name for roles
 * @param email         User email. {@code null} for ROLE subjects.
 * @param description   Role description. {@code null} for USER subjects.
 * @param status        Identity Service status: {@code "ACTIVE"} or {@code "INACTIVE"}
 * @param version       Monotonically increasing domain version; used for idempotency
 * @param deleted       {@code true} = deactivated; {@code false} = active
 */
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
