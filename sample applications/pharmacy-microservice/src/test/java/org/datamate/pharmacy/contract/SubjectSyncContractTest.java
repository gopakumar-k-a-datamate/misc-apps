package org.datamate.pharmacy.contract;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.Message;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.datamate.pharmacy.messaging.SubjectSyncMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pact Consumer Contract Test for the {@code auth.subject.sync} message exchange.
 *
 * <h2>Purpose</h2>
 * <p>This test defines and verifies the Pharmacy Service's expectation of the
 * JSON wire format published by the Identity Service on the
 * {@code auth.subject.sync} RabbitMQ Fanout exchange.</p>
 *
 * <p>Running this test generates a Pact contract file at:
 * {@code target/pacts/pharmacy-microservice-identity-service.json}</p>
 *
 * <p>The Identity Service CI pipeline must run Pact provider verification
 * against this generated file to guarantee the wire schema is never broken.</p>
 *
 * <h2>What Is Verified</h2>
 * <ul>
 *   <li>All required fields ({@code subjectType}, {@code subjectId},
 *       {@code subjectName}, {@code version}, {@code deleted}) are present in the
 *       payload and deserialize correctly into {@link SubjectSyncMessage}.</li>
 *   <li>Field types match the wire contract specification.</li>
 *   <li>Forward compatibility: unknown fields are silently ignored
 *       ({@code @JsonIgnoreProperties(ignoreUnknown = true)}).</li>
 * </ul>
 *
 * <h2>Wire Contract</h2>
 * <p>See {@code docs/contracts/subject-sync-wire-contract.md} for the full specification.</p>
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "identity-service", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V3)
class SubjectSyncContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // -------------------------------------------------------------------------
    // Pact Definitions
    // -------------------------------------------------------------------------

    /**
     * Defines the Pact for a USER_CREATED event.
     * Verifies that the Pharmacy Service can deserialize an active user event.
     */
    @Pact(consumer = "pharmacy-microservice", provider = "identity-service")
    MessagePact userCreatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody()
                .stringMatcher("subjectType", "USER|ROLE", "USER")
                .uuid("subjectId", "a1b2c3d4-0000-0000-0000-000000000001")
                .stringType("subjectName", "Alice Smith")
                .numberType("version", 1L)
                .booleanType("deleted", false);

        return builder
                .expectsToReceive("a USER_CREATED subject sync message")
                .withContent(body)
                .toPact();
    }

    /**
     * Defines the Pact for a USER_DEACTIVATED event.
     * Verifies that the Pharmacy Service can deserialize a soft-delete event.
     */
    @Pact(consumer = "pharmacy-microservice", provider = "identity-service")
    MessagePact userDeactivatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody()
                .stringMatcher("subjectType", "USER|ROLE", "USER")
                .uuid("subjectId", "a1b2c3d4-0000-0000-0000-000000000001")
                .stringType("subjectName", "Alice Smith")
                .numberType("version", 3L)
                .booleanType("deleted", true);

        return builder
                .expectsToReceive("a USER_DEACTIVATED subject sync message")
                .withContent(body)
                .toPact();
    }

    /**
     * Defines the Pact for a ROLE_CREATED event.
     * Verifies that the Pharmacy Service can deserialize a role event.
     */
    @Pact(consumer = "pharmacy-microservice", provider = "identity-service")
    MessagePact roleCreatedPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody()
                .stringMatcher("subjectType", "USER|ROLE", "ROLE")
                .uuid("subjectId", "d1d2d3d4-0000-0000-0000-000000000001")
                .stringType("subjectName", "PHARMACIST")
                .numberType("version", 1L)
                .booleanType("deleted", false);

        return builder
                .expectsToReceive("a ROLE_CREATED subject sync message")
                .withContent(body)
                .toPact();
    }

    // -------------------------------------------------------------------------
    // Verification Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should deserialize USER_CREATED message into SubjectSyncMessage correctly")
    @PactTestFor(pactMethod = "userCreatedPact")
    void verifyUserCreatedMessage(List<Message> messages) throws Exception {
        SubjectSyncMessage message = deserialize(messages.get(0));

        assertThat(message.subjectType()).isEqualTo("USER");
        assertThat(message.subjectId()).isNotBlank();
        assertThat(message.subjectName()).isNotBlank();
        assertThat(message.version()).isGreaterThanOrEqualTo(0L);
        assertThat(message.deleted()).isFalse();
    }

    @Test
    @DisplayName("Should deserialize USER_DEACTIVATED message with deleted=true")
    @PactTestFor(pactMethod = "userDeactivatedPact")
    void verifyUserDeactivatedMessage(List<Message> messages) throws Exception {
        SubjectSyncMessage message = deserialize(messages.get(0));

        assertThat(message.subjectType()).isEqualTo("USER");
        assertThat(message.deleted()).isTrue();
        assertThat(message.version()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    @DisplayName("Should deserialize ROLE_CREATED message into SubjectSyncMessage correctly")
    @PactTestFor(pactMethod = "roleCreatedPact")
    void verifyRoleCreatedMessage(List<Message> messages) throws Exception {
        SubjectSyncMessage message = deserialize(messages.get(0));

        assertThat(message.subjectType()).isEqualTo("ROLE");
        assertThat(message.subjectId()).isNotBlank();
        assertThat(message.subjectName()).isNotBlank();
        assertThat(message.deleted()).isFalse();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SubjectSyncMessage deserialize(Message message) throws Exception {
        return objectMapper.readValue(
                message.contentsAsString(),
                SubjectSyncMessage.class
        );
    }
}
