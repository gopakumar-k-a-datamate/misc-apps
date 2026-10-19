package org.datamate.identity.shared.messaging;

import org.datamate.identity.identity.domain.event.role.RoleActivatedEvent;
import org.datamate.identity.identity.domain.event.role.RoleCreatedEvent;
import org.datamate.identity.identity.domain.event.role.RoleDeactivatedEvent;
import org.datamate.identity.identity.domain.event.role.RoleUpdatedEvent;
import org.datamate.identity.identity.domain.event.user.UserActivatedEvent;
import org.datamate.identity.identity.domain.event.user.UserCreatedEvent;
import org.datamate.identity.identity.domain.event.user.UserDeactivatedEvent;
import org.datamate.identity.identity.domain.event.user.UserInformationUpdatedEvent;
import org.datamate.identity.shared.config.messaging.RabbitConfig;
import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens to internal Identity Service domain events and broadcasts them to
 * the {@code auth.subject.sync} RabbitMQ Fanout exchange as {@link SubjectSyncMessage}.
 *
 * <p>Implements the Transactional Outbox pattern via {@code @TransactionalEventListener}
 * with {@code AFTER_COMMIT} phase, guaranteeing that messages are published only
 * after the database transaction has successfully committed.</p>
 *
 * <p><strong>ACL Boundary:</strong> This class is intentionally decoupled from the
 * {@code authz-core} library. It uses the Identity Service's own {@link SubjectSyncMessage}
 * record, whose JSON wire format is governed by
 * {@code docs/contracts/subject-sync-wire-contract.md}.</p>
 *
 * <p><strong>Unified Subject Model:</strong> Each event handler extracts the full set of
 * subject fields from the domain event. Fields not applicable to a subject type
 * are explicitly set to {@code null} (e.g. email for ROLE events).</p>
 */
@Component
public class AuthzSubjectEventPublisher {

    @EnableLogger
    private Logger log;

    private final MessagePublisher messagePublisher;

    public AuthzSubjectEventPublisher(MessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }

    // --- User Events ---

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserCreatedEvent event) {
        publish(buildUserMessage(
                event.aggregateId().toString(),
                event.userName(),
                event.firstName(),
                event.lastName(),
                event.email(),
                event.status().name(),
                event.domainVersion(),
                false
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserInformationUpdatedEvent event) {
        publish(buildUserMessage(
                event.aggregateId().toString(),
                event.userName(),
                event.firstName(),
                event.lastName(),
                event.email(),
                "ACTIVE",         // update events only fire for active users
                event.domainVersion(),
                false
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserActivatedEvent event) {
        publish(buildUserMessage(
                event.aggregateId().toString(),
                event.userName(),
                event.firstName(),
                event.lastName(),
                event.email(),
                "ACTIVE",
                event.domainVersion(),
                false
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserDeactivatedEvent event) {
        publish(buildUserMessage(
                event.aggregateId().toString(),
                event.userName(),
                event.firstName(),
                event.lastName(),
                event.email(),
                "INACTIVE",
                event.domainVersion(),
                true
        ));
    }

    // --- Role Events ---

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(RoleCreatedEvent event) {
        publish(buildRoleMessage(
                event.aggregateId().toString(),
                event.name(),
                event.description(),
                event.status().name(),
                event.domainVersion(),
                false
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(RoleUpdatedEvent event) {
        publish(buildRoleMessage(
                event.aggregateId().toString(),
                event.name(),
                event.description(),
                "ACTIVE",
                event.domainVersion(),
                false
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(RoleActivatedEvent event) {
        publish(buildRoleMessage(
                event.aggregateId().toString(),
                event.name(),
                null,             // description not carried in activation event
                "ACTIVE",
                event.domainVersion(),
                false
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(RoleDeactivatedEvent event) {
        publish(buildRoleMessage(
                event.aggregateId().toString(),
                event.name(),
                null,             // description not carried in deactivation event
                "INACTIVE",
                event.domainVersion(),
                true
        ));
    }

    // --- Private Builders & Publisher ---

    private SubjectSyncMessage buildUserMessage(String id, String userName, String firstName, String lastName, String email, String status, long version, boolean deleted) {
        return new SubjectSyncMessage(
                "USER",
                id,
                userName,
                firstName + " " + lastName,
                email,
                null,             // description: not applicable for USER
                status,
                version,
                deleted
        );
    }

    private SubjectSyncMessage buildRoleMessage(String id, String name, String description, String status, long version, boolean deleted) {
        return new SubjectSyncMessage(
                "ROLE",
                id,
                name,
                name,             // displayName = name for roles
                null,             // email: not applicable for ROLE
                description,
                status,
                version,
                deleted
        );
    }

    private void publish(SubjectSyncMessage message) {
        log.info("Publishing SubjectSyncMessage: type={}, id={}, status={}, version={}, deleted={}",
                message.subjectType(), message.subjectId(), message.status(), message.version(), message.deleted());
        messagePublisher.publish(RabbitConfig.AUTHZ_SUBJECT_SYNC_EXCHANGE, "", message);
    }
}
