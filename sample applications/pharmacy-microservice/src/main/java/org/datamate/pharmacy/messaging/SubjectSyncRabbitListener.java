package org.datamate.pharmacy.messaging;

import org.datamate.authz.api.subject.SubjectManagementService;
import org.datamate.authz.event.AuthzSubjectSyncCommand;
import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ listener that subscribes to the {@code auth.subject.sync} Fanout exchange
 * and routes inbound subject lifecycle events into the {@code authz-core} framework.
 *
 * <h2>Anti-Corruption Layer (ACL)</h2>
 * <p>This listener acts as the boundary between the external messaging world and the
 * internal authorization framework. It follows the ACL pattern from Domain-Driven Design:</p>
 * <ol>
 *   <li>Receives raw JSON from RabbitMQ and deserializes it into the Pharmacy Service's
 *       own {@link SubjectSyncMessage} record (not an authz-core class).</li>
 *   <li>Maps the {@link SubjectSyncMessage} to the framework's internal
 *       {@link AuthzSubjectSyncCommand}.</li>
 *   <li>Delegates to {@link SubjectManagementService#apply(AuthzSubjectSyncCommand)} for
 *       all persistence logic (upsert, soft-delete, idempotency checks).</li>
 * </ol>
 *
 * <h2>Queue Strategy</h2>
 * <p>Uses an anonymous, auto-delete queue so every Pharmacy Service instance
 * receives every event independently. This is appropriate for local projections
 * where each instance maintains its own local database copy.</p>
 *
 * <h2>Wire Contract</h2>
 * <p>The expected JSON format is defined in:
 * {@code docs/contracts/subject-sync-wire-contract.md}</p>
 *
 * <h2>Contract Tests</h2>
 * <p>The JSON deserialization expectations for this listener are verified by
 * Pact consumer contract tests in:
 * {@code SubjectSyncContractTest} under {@code src/test/java/.../contract/}</p>
 */
@Component
public class SubjectSyncRabbitListener {

    @EnableLogger
    private Logger log;

    private final SubjectManagementService subjectManagementService;

    public SubjectSyncRabbitListener(SubjectManagementService subjectManagementService) {
        this.subjectManagementService = subjectManagementService;
    }

    /**
     * Receives a subject sync message from the Identity Service, maps it to
     * the framework's event model, and applies the change to the local projection.
     *
     * @param message the deserialized subject sync payload from RabbitMQ
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue, // Anonymous auto-delete queue — every instance receives every event
            exchange = @Exchange(value = "auth.subject.sync", type = ExchangeTypes.FANOUT)
    ))
    public void onSubjectSync(SubjectSyncMessage message) {
        log.debug("Received SubjectSyncMessage: type={}, id={}, version={}, deleted={}",
                message.subjectType(), message.subjectId(), message.version(), message.deleted());

        // ACL Mapping: translate pharmacy's local DTO into the framework's internal event
        AuthzSubjectSyncCommand event = new AuthzSubjectSyncCommand(
                message.subjectType(),
                message.subjectId(),
                message.subjectName(),
                message.displayName(),
                message.email(),
                message.description(),
                message.status(),
                message.version(),
                message.deleted()
        );

        // Delegate all persistence and idempotency logic to the framework
        subjectManagementService.apply(event);
    }
}
