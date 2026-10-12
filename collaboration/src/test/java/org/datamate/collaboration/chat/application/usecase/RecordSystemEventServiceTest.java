package org.datamate.collaboration.chat.application.usecase;

import org.datamate.collaboration.chat.application.dto.MessageDto;
import org.datamate.collaboration.chat.application.dto.RecordSystemEventCommand;
import org.datamate.collaboration.chat.application.mapper.MessageMapper;
import org.datamate.collaboration.chat.application.port.out.MessageBroadcastPort;
import org.datamate.collaboration.chat.application.port.out.MessageRepositoryPort;
import org.datamate.collaboration.chat.application.port.out.ThreadRepositoryPort;
import org.datamate.collaboration.chat.domain.model.Message;
import org.datamate.collaboration.chat.domain.model.Thread;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecordSystemEventService Unit Tests")
class RecordSystemEventServiceTest {

    @Mock
    private MessageRepositoryPort messageRepository;

    @Mock
    private ThreadRepositoryPort threadRepository;

    @Mock
    private MessageBroadcastPort messageBroadcastPort;

    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private RecordSystemEventService recordSystemEventService;

    private UUID threadId;
    private String eventText;

    @BeforeEach
    void setUp() {
        threadId = UUID.randomUUID();
        eventText = "Prescription #8493 has been verified by Pharmacy System";
    }

    @Test
    @DisplayName("Should successfully record system event, persist with isSystemMessage=true, and broadcast to WebSocket")
    void shouldRecordSystemEvent_Successfully_WithSystemFlag_AndBroadcastToWebSocket() {
        RecordSystemEventCommand command = new RecordSystemEventCommand(threadId, "PHARMACY_SERVICE", eventText, null);
        when(threadRepository.existsById(threadId)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID generatedMessageId = UUID.randomUUID();
        MessageDto expectedDto = new MessageDto(
                generatedMessageId,
                null,
                "PHARMACY_SERVICE",
                eventText,
                false,
                true,
                null,
                Instant.now()
        );
        when(messageMapper.toDto(any(Message.class), isNull())).thenReturn(expectedDto);

        MessageDto result = recordSystemEventService.recordSystemEvent(command);

        assertThat(result).isNotNull();
        assertThat(result.isSystemMessage()).isTrue();
        assertThat(result.senderId()).isEqualTo("PHARMACY_SERVICE");
        assertThat(result.text()).isEqualTo(eventText);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        Message savedDomainMessage = messageCaptor.getValue();

        assertThat(savedDomainMessage.getThreadId()).isEqualTo(threadId);
        assertThat(savedDomainMessage.getSenderId()).isEqualTo("PHARMACY_SERVICE");
        assertThat(savedDomainMessage.getText()).isEqualTo(eventText);
        assertThat(savedDomainMessage.isSystemMessage()).isTrue();
        assertThat(savedDomainMessage.isFile()).isFalse();
        assertThat(savedDomainMessage.getAttachmentId()).isNull();

        verify(messageBroadcastPort).broadcastMessage(eq(threadId), eq(expectedDto));
    }

    @Test
    @DisplayName("Should lazily create thread when thread does not exist")
    void shouldLazilyCreateThread_WhenThreadDoesNotExist() {
        RecordSystemEventCommand command = RecordSystemEventCommand.of(threadId, eventText);
        when(threadRepository.existsById(threadId)).thenReturn(false);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageDto expectedDto = new MessageDto(
                UUID.randomUUID(), null, "SYSTEM", eventText, false, true, null, Instant.now()
        );
        when(messageMapper.toDto(any(Message.class), isNull())).thenReturn(expectedDto);

        recordSystemEventService.recordSystemEvent(command);

        ArgumentCaptor<Thread> threadCaptor = ArgumentCaptor.forClass(Thread.class);
        verify(threadRepository).save(threadCaptor.capture());
        assertThat(threadCaptor.getValue().getId()).isEqualTo(threadId);
    }

    @Test
    @DisplayName("Should handle concurrent thread creation gracefully without failing")
    void shouldHandleConcurrentThreadCreation_Gracefully() {
        RecordSystemEventCommand command = RecordSystemEventCommand.of(threadId, eventText);
        when(threadRepository.existsById(threadId)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("Unique constraint violation"))
                .when(threadRepository).save(any(Thread.class));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageDto expectedDto = new MessageDto(
                UUID.randomUUID(), null, "SYSTEM", eventText, false, true, null, Instant.now()
        );
        when(messageMapper.toDto(any(Message.class), isNull())).thenReturn(expectedDto);

        MessageDto result = recordSystemEventService.recordSystemEvent(command);

        assertThat(result).isNotNull();
        verify(messageRepository).save(any(Message.class));
        verify(messageBroadcastPort).broadcastMessage(eq(threadId), eq(expectedDto));
    }

    @Test
    @DisplayName("Should not create thread when thread already exists")
    void shouldNotCreateThread_WhenThreadAlreadyExists() {
        RecordSystemEventCommand command = RecordSystemEventCommand.of(threadId, eventText);
        when(threadRepository.existsById(threadId)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        recordSystemEventService.recordSystemEvent(command);

        verify(threadRepository, never()).save(any(Thread.class));
    }

    @Test
    @DisplayName("Should default senderId to SYSTEM when senderId is null or blank")
    void shouldDefaultSenderIdToSYSTEM_WhenSenderIdIsNull() {
        RecordSystemEventCommand command = new RecordSystemEventCommand(threadId, null, eventText, null);
        when(threadRepository.existsById(threadId)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        recordSystemEventService.recordSystemEvent(command);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getSenderId()).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("Should throw ApplicationValidationException when command is null")
    void shouldThrowException_WhenCommandIsNull() {
        assertThatThrownBy(() -> recordSystemEventService.recordSystemEvent(null))
                .isInstanceOf(ApplicationValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", CollaborationErrorCodes.REQUIRED_FIELD_MISSING.code());
    }

    @Test
    @DisplayName("Should throw ApplicationValidationException when threadId is null")
    void shouldThrowException_WhenThreadIdIsNull() {
        RecordSystemEventCommand command = new RecordSystemEventCommand(null, "SYSTEM", eventText, null);

        assertThatThrownBy(() -> recordSystemEventService.recordSystemEvent(command))
                .isInstanceOf(ApplicationValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", CollaborationErrorCodes.REQUIRED_FIELD_MISSING.code());
    }

    @Test
    @DisplayName("Should throw ApplicationValidationException when text is null or blank")
    void shouldThrowException_WhenTextIsBlank() {
        RecordSystemEventCommand commandWithBlank = new RecordSystemEventCommand(threadId, "SYSTEM", "   ", null);

        assertThatThrownBy(() -> recordSystemEventService.recordSystemEvent(commandWithBlank))
                .isInstanceOf(ApplicationValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", CollaborationErrorCodes.REQUIRED_FIELD_MISSING.code());
    }
}
