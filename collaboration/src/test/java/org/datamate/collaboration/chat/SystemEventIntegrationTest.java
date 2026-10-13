package org.datamate.collaboration.chat;

import org.datamate.authz.enforcement.PolicyEnforcer;
import org.datamate.authz.starter.config.BedrockAuthzAutoConfiguration;
import org.datamate.collaboration.chat.adapter.in.messaging.SystemEventRabbitListener;
import org.datamate.collaboration.chat.adapter.in.messaging.dto.ChatSystemEventPayload;
import org.datamate.collaboration.chat.adapter.out.persistence.message.entity.MessageJpaEntity;
import org.datamate.collaboration.chat.adapter.out.persistence.message.repository.MessageJpaRepository;
import org.datamate.collaboration.chat.adapter.out.persistence.thread.repository.ThreadJpaRepository;
import org.datamate.collaboration.chat.application.dto.MessageDto;
import org.datamate.collaboration.chat.application.dto.RecordSystemEventCommand;
import org.datamate.collaboration.chat.application.port.in.RecordSystemEventUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * End-to-end Integration Test for system audit logs and background status events.
 * <p>
 * Verifies that incoming broker events are received, processed, persisted to the database
 * as system messages, and broadcasted to active WebSocket subscribers.
 */
@SpringBootTest
@EnableAutoConfiguration(exclude = {BedrockAuthzAutoConfiguration.class})
@DisplayName("System Event Integration Tests")
class SystemEventIntegrationTest {

    @MockBean
    private PolicyEnforcer policyEnforcer;

    @Autowired
    private RecordSystemEventUseCase recordSystemEventUseCase;

    @Autowired
    private MessageJpaRepository messageJpaRepository;

    @Autowired
    private ThreadJpaRepository threadJpaRepository;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SystemEventRabbitListener systemEventRabbitListener;

    @Test
    @DisplayName("Should consume simulated broker event, save to database as system message, and broadcast to WebSocket")
    void shouldPersistSystemEventToDatabase_AndBroadcastToWebSocketTopic() {
        UUID threadId = UUID.randomUUID();
        String eventMessage = "Payment of $45.00 confirmed for Order #8821 by Finance Module";
        RecordSystemEventCommand command = new RecordSystemEventCommand(threadId, "FINANCE_SERVICE", eventMessage, null);

        // Execute use case (simulating the broker consumer delegating to the inbound port)
        MessageDto resultDto = recordSystemEventUseCase.recordSystemEvent(command);

        // 1. Verify returned DTO
        assertThat(resultDto).isNotNull();
        assertThat(resultDto.id()).isNotNull();
        assertThat(resultDto.isSystemMessage()).isTrue();
        assertThat(resultDto.isFile()).isFalse();
        assertThat(resultDto.senderId()).isEqualTo("FINANCE_SERVICE");
        assertThat(resultDto.text()).isEqualTo(eventMessage);

        // 2. Verify lazy Thread creation in database
        assertThat(threadJpaRepository.findById(threadId)).isPresent();

        // 3. Verify Message persistence in database with is_system_message = true
        Optional<MessageJpaEntity> savedEntityOpt = messageJpaRepository.findById(resultDto.id());
        assertThat(savedEntityOpt).isPresent();

        MessageJpaEntity savedEntity = savedEntityOpt.get();
        assertThat(savedEntity.getId()).isEqualTo(resultDto.id());
        assertThat(savedEntity.getThreadId()).isEqualTo(threadId);
        assertThat(savedEntity.getSenderId()).isEqualTo("FINANCE_SERVICE");
        assertThat(savedEntity.getText()).isEqualTo(eventMessage);
        assertThat(savedEntity.isSystemMessage()).isTrue();
        assertThat(savedEntity.isFile()).isFalse();
        assertThat(savedEntity.getAttachmentId()).isNull();
        assertThat(savedEntity.getTimestamp()).isNotNull();

        // 4. Verify real-time broadcast to active STOMP WebSocket topic
        verify(messagingTemplate).convertAndSend(eq("/topic/thread." + threadId), any(MessageDto.class));
    }

    @Test
    @DisplayName("Should process incoming broker event payload via RabbitMQ listener adapter end-to-end")
    void shouldProcessIncomingBrokerEvent_ViaListener_EndToEnd() {
        UUID threadId = UUID.randomUUID();
        String eventText = "Canteen order #3021 prepared for pickup";
        ChatSystemEventPayload payload = new ChatSystemEventPayload(
                threadId,
                "CANTEEN_SYSTEM",
                "ORDER_STATUS_CHANGED",
                eventText,
                null,
                null,
                Instant.now()
        );

        // Simulate incoming broker delivery to the consumer
        systemEventRabbitListener.onSystemEvent(payload);

        // Verify thread lazily created
        assertThat(threadJpaRepository.findById(threadId)).isPresent();

        // Verify message persisted as system message
        List<MessageJpaEntity> messages = messageJpaRepository.findByThreadIdOrderByTimestampDesc(
                threadId, PageRequest.of(0, 10)).getContent();
        assertThat(messages).hasSize(1);
        MessageJpaEntity savedMessage = messages.get(0);
        assertThat(savedMessage.getSenderId()).isEqualTo("CANTEEN_SYSTEM");
        assertThat(savedMessage.getText()).isEqualTo(eventText);
        assertThat(savedMessage.isSystemMessage()).isTrue();
        assertThat(savedMessage.isFile()).isFalse();

        // Verify WebSocket broadcast
        verify(messagingTemplate).convertAndSend(eq("/topic/thread." + threadId), any(MessageDto.class));
    }
}
