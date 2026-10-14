package org.datamate.collaboration.chat.adapter.in.messaging;

import org.datamate.collaboration.chat.adapter.in.messaging.dto.ChatSystemEventPayload;
import org.datamate.collaboration.chat.application.dto.RecordSystemEventCommand;
import org.datamate.collaboration.chat.application.port.in.RecordSystemEventUseCase;
import org.datamate.collaboration.exception.ApplicationValidationException;
import org.datamate.collaboration.exception.CollaborationErrorCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemEventRabbitListener Unit Tests")
class SystemEventRabbitListenerTest {

    @Mock
    private RecordSystemEventUseCase recordSystemEventUseCase;

    @InjectMocks
    private SystemEventRabbitListener listener;

    private UUID threadId;

    @BeforeEach
    void setUp() {
        threadId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should successfully consume broker event and delegate command to use case")
    void shouldConsumeBrokerEvent_AndDelegateToUseCase() {
        ChatSystemEventPayload payload = new ChatSystemEventPayload(
                threadId,
                "CANTEEN_SYSTEM",
                "ORDER_PREPARED",
                "Order #5521 is ready for pick-up",
                null,
                null,
                Instant.now()
        );

        listener.onSystemEvent(payload);

        ArgumentCaptor<RecordSystemEventCommand> captor = ArgumentCaptor.forClass(RecordSystemEventCommand.class);
        verify(recordSystemEventUseCase).recordSystemEvent(captor.capture());

        RecordSystemEventCommand command = captor.getValue();
        assertThat(command.threadId()).isEqualTo(threadId);
        assertThat(command.senderId()).isEqualTo("CANTEEN_SYSTEM");
        assertThat(command.text()).isEqualTo("Order #5521 is ready for pick-up");
        assertThat(command.parentId()).isNull();
    }

    @Test
    @DisplayName("Should prioritize text over message when both are provided")
    void shouldPrioritizeText_OverMessage() {
        ChatSystemEventPayload payload = new ChatSystemEventPayload(
                threadId,
                "BILLING_SERVICE",
                "INVOICE_GENERATED",
                "Fallback message",
                "Primary text body",
                null,
                Instant.now()
        );

        listener.onSystemEvent(payload);

        ArgumentCaptor<RecordSystemEventCommand> captor = ArgumentCaptor.forClass(RecordSystemEventCommand.class);
        verify(recordSystemEventUseCase).recordSystemEvent(captor.capture());

        RecordSystemEventCommand command = captor.getValue();
        assertThat(command.text()).isEqualTo("Primary text body");
    }

    @Test
    @DisplayName("Should default senderId to SYSTEM when sourceService is null or blank")
    void shouldDefaultSenderIdToSYSTEM_WhenSourceServiceMissing() {
        ChatSystemEventPayload payload = new ChatSystemEventPayload(
                threadId,
                null,
                "STATUS_UPDATE",
                "System maintenance alert",
                null,
                null,
                Instant.now()
        );

        listener.onSystemEvent(payload);

        ArgumentCaptor<RecordSystemEventCommand> captor = ArgumentCaptor.forClass(RecordSystemEventCommand.class);
        verify(recordSystemEventUseCase).recordSystemEvent(captor.capture());

        RecordSystemEventCommand command = captor.getValue();
        assertThat(command.senderId()).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("Should handle null payload gracefully without calling use case")
    void shouldIgnoreNullPayload() {
        listener.onSystemEvent(null);

        verify(recordSystemEventUseCase, never()).recordSystemEvent(any());
    }

    @Test
    @DisplayName("Should catch ApplicationValidationException and not rethrow")
    void shouldCatchApplicationValidationException_WithoutFailing() {
        ChatSystemEventPayload payload = new ChatSystemEventPayload(
                null,
                "SOURCE",
                "TYPE",
                "text",
                null,
                null,
                Instant.now()
        );

        doThrow(new ApplicationValidationException(CollaborationErrorCodes.REQUIRED_FIELD_MISSING.code(), "threadId"))
                .when(recordSystemEventUseCase).recordSystemEvent(any());

        listener.onSystemEvent(payload);

        verify(recordSystemEventUseCase).recordSystemEvent(any());
    }
}
