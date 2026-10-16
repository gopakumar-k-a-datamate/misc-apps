package org.datamate.collaboration.chat.application.usecase;

import com.datamate.bedrock.framework.storage.application.port.StorageService;
import com.datamate.bedrock.framework.storage.domain.model.StorageObject;
import org.datamate.collaboration.chat.application.dto.SendMessageCommand;
import org.datamate.collaboration.chat.application.dto.SendMessageRequest;
import org.datamate.collaboration.chat.application.mapper.MessageMapper;
import org.datamate.collaboration.chat.application.port.out.AttachmentRepositoryPort;
import org.datamate.collaboration.chat.application.port.out.DocumentConversionPort;
import org.datamate.collaboration.chat.application.port.out.MessageBroadcastPort;
import org.datamate.collaboration.chat.application.port.out.MessageRepositoryPort;
import org.datamate.collaboration.chat.application.port.out.ThreadRepositoryPort;
import org.datamate.collaboration.chat.domain.model.Attachment;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SendMessageService Unit Tests")
class SendMessageServiceTest {

    @Mock
    private MessageRepositoryPort messageRepository;

    @Mock
    private ThreadRepositoryPort threadRepository;

    @Mock
    private AttachmentRepositoryPort attachmentRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private DocumentConversionPort documentConversionPort;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private MessageBroadcastPort messageBroadcastPort;

    @InjectMocks
    private SendMessageService sendMessageService;

    private UUID threadId;
    private String senderId;
    private String text;
    private SendMessageCommand request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sendMessageService, "defaultBucket", "chat-attachments");
        threadId = UUID.randomUUID();
        senderId = "user-john";
        text = "Hello, World!";
        request = new SendMessageCommand(text);
    }

    @Test
    @DisplayName("should lazily create thread when it does not exist")
    void shouldCreateThread_WhenThreadDoesNotExist() {
        when(threadRepository.existsById(threadId)).thenReturn(false);

        sendMessageService.sendMessage(threadId, senderId, request);

        ArgumentCaptor<Thread> threadCaptor = ArgumentCaptor.forClass(Thread.class);
        verify(threadRepository).save(threadCaptor.capture());
        assertThat(threadCaptor.getValue().getId()).isEqualTo(threadId);
    }

    @Test
    @DisplayName("should skip thread creation when thread already exists (idempotent)")
    void shouldNotCreateThread_WhenThreadAlreadyExists() {
        when(threadRepository.existsById(threadId)).thenReturn(true);

        sendMessageService.sendMessage(threadId, senderId, request);

        verify(threadRepository, never()).save(any(Thread.class));
    }

    @Test
    @DisplayName("should handle concurrent thread creation gracefully")
    void shouldHandleConcurrentThreadCreation() {
        when(threadRepository.existsById(threadId)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("Duplicate key"))
                .when(threadRepository).save(any(Thread.class));

        sendMessageService.sendMessage(threadId, senderId, request);

        verify(messageRepository).save(any(Message.class));
    }

    @Test
    @DisplayName("should persist text message with correct fields")
    void shouldPersistTextMessage_WithCorrectFields() {
        when(threadRepository.existsById(threadId)).thenReturn(true);

        sendMessageService.sendMessage(threadId, senderId, request);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());

        Message saved = messageCaptor.getValue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getThreadId()).isEqualTo(threadId);
        assertThat(saved.getSenderId()).isEqualTo(senderId);
        assertThat(saved.getText()).isEqualTo(text);
        assertThat(saved.isFile()).isFalse();
        assertThat(saved.isSystemMessage()).isFalse();
        assertThat(saved.getAttachmentId()).isNull();
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("should persist message with attachment when attachmentUrls is provided (RMS flow)")
    void shouldPersistMessage_WithAttachmentUrls_WhenProvided() {
        when(threadRepository.existsById(threadId)).thenReturn(true);
        when(storageService.getMetadata(eq("chat-attachments"), eq("12345_document.pdf")))
                .thenReturn(StorageObject.builder()
                        .bucketName("chat-attachments")
                        .objectKey("12345_document.pdf")
                        .contentType("application/pdf")
                        .size(2048L)
                        .build());

        SendMessageCommand attachmentRequest = new SendMessageCommand(
                "Here is the document",
                List.of("http://localhost:9000/chat-attachments/12345_document.pdf")
        );

        sendMessageService.sendMessage(threadId, senderId, attachmentRequest);

        ArgumentCaptor<Attachment> attachmentCaptor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentRepository).save(attachmentCaptor.capture());
        Attachment savedAttachment = attachmentCaptor.getValue();
        assertThat(savedAttachment.getFileName()).isEqualTo("document.pdf");
        assertThat(savedAttachment.getMimeType()).isEqualTo("application/pdf");
        assertThat(savedAttachment.getFileSize()).isEqualTo(2048L);
        assertThat(savedAttachment.getUploadUrl()).isEqualTo("http://localhost:9000/chat-attachments/12345_document.pdf");

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        Message savedMessage = messageCaptor.getValue();
        assertThat(savedMessage.isFile()).isTrue();
        assertThat(savedMessage.getAttachmentId()).isEqualTo(savedAttachment.getId());
        assertThat(savedMessage.getText()).isEqualTo("Here is the document");
    }

    @Test
    @DisplayName("should convert office document to PDF preview when convertible")
    void shouldConvertOfficeDocument_ToPdfPreview_WhenConvertible() {
        when(threadRepository.existsById(threadId)).thenReturn(true);
        when(documentConversionPort.isConvertible("document.docx")).thenReturn(true);
        when(storageService.download(eq("chat-attachments"), eq("document.docx")))
                .thenReturn(new ByteArrayInputStream("dummy docx content".getBytes()));
        when(documentConversionPort.convertToPdf(any(), eq("document.docx")))
                .thenReturn("dummy pdf bytes".getBytes());
        when(storageService.upload(eq("chat-attachments"), anyString(), any(), eq("application/pdf"), anyLong()))
                .thenReturn(StorageObject.builder()
                        .bucketName("chat-attachments")
                        .objectKey("previews/" + threadId + "/document.docx.pdf")
                        .build());

        SendMessageCommand docxRequest = new SendMessageCommand(
                "Here is the docx",
                List.of("http://localhost:9000/chat-attachments/document.docx")
        );

        sendMessageService.sendMessage(threadId, senderId, docxRequest);

        ArgumentCaptor<Attachment> attachmentCaptor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentRepository).save(attachmentCaptor.capture());
        Attachment savedAttachment = attachmentCaptor.getValue();
        assertThat(savedAttachment.getPreviewUrl()).isEqualTo("previews/" + threadId + "/document.docx.pdf");
    }

    @Test
    @DisplayName("should persist message when only attachmentUrls is provided and text is empty (captionless attachment)")
    void shouldPersistMessage_WhenOnlyAttachmentUrlsProvided() {
        when(threadRepository.existsById(threadId)).thenReturn(true);

        SendMessageCommand captionlessRequest = new SendMessageCommand(
                null,
                List.of("http://localhost:9000/chat-attachments/photo.png")
        );

        sendMessageService.sendMessage(threadId, senderId, captionlessRequest);

        verify(attachmentRepository).save(any(Attachment.class));

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        Message saved = messageCaptor.getValue();
        assertThat(saved.isFile()).isTrue();
        assertThat(saved.getAttachmentId()).isNotNull();
        assertThat(saved.getText()).isNull();
    }

    @Test
    @DisplayName("should persist message when direct attachmentId is provided")
    void shouldPersistMessage_WhenDirectAttachmentIdProvided() {
        when(threadRepository.existsById(threadId)).thenReturn(true);
        UUID existingAttachmentId = UUID.randomUUID();

        SendMessageCommand directIdRequest = new SendMessageCommand("Message with existing ID", existingAttachmentId);

        sendMessageService.sendMessage(threadId, senderId, directIdRequest);

        verify(attachmentRepository, never()).save(any(Attachment.class));

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        Message saved = messageCaptor.getValue();
        assertThat(saved.isFile()).isTrue();
        assertThat(saved.getAttachmentId()).isEqualTo(existingAttachmentId);
    }

    @Test
    @DisplayName("should reject when command is null")
    void shouldReject_WhenCommandIsNull() {
        SendMessageCommand nullCommand = null;

        assertThatThrownBy(() -> sendMessageService.sendMessage(threadId, senderId, nullCommand))
                .isInstanceOf(ApplicationValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", CollaborationErrorCodes.REQUIRED_FIELD_MISSING.code());
    }

    @Test
    @DisplayName("should reject blocked file extension in attachmentUrls")
    void shouldRejectBlockedFileExtension_InAttachmentUrls() {
        SendMessageCommand blockedRequest = new SendMessageCommand(
                "Malicious file",
                List.of("http://localhost:9000/chat-attachments/trojan.exe")
        );

        assertThatThrownBy(() -> sendMessageService.sendMessage(threadId, senderId, blockedRequest))
                .isInstanceOf(ApplicationValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", CollaborationErrorCodes.FILE_TYPE_BLOCKED.code());
    }

    @Test
    @DisplayName("should reject when both text and attachment are missing")
    void shouldReject_WhenBothTextAndAttachmentMissing() {
        SendMessageCommand emptyRequest = new SendMessageCommand("   ", null, null, null);

        assertThatThrownBy(() -> sendMessageService.sendMessage(threadId, senderId, emptyRequest))
                .isInstanceOf(ApplicationValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", CollaborationErrorCodes.REQUIRED_FIELD_MISSING.code());
    }

    @Test
    @DisplayName("should reject when attachment URL is blank")
    void shouldReject_WhenAttachmentUrlIsBlank() {
        SendMessageCommand blankUrlRequest = new SendMessageCommand(
                null,
                List.of("   ")
        );

        assertThatThrownBy(() -> sendMessageService.sendMessage(threadId, senderId, blankUrlRequest))
                .isInstanceOf(ApplicationValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", CollaborationErrorCodes.FIELD_BLANK.code());
    }

    @Test
    @DisplayName("should persist message when legacy SendMessageRequest overload is invoked")
    void shouldPersistMessage_WhenSendMessageRequestProvided() {
        when(threadRepository.existsById(threadId)).thenReturn(true);
        SendMessageRequest legacyRequest = new SendMessageRequest("Hello from legacy request");

        sendMessageService.sendMessage(threadId, senderId, legacyRequest);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        Message saved = messageCaptor.getValue();
        assertThat(saved.getText()).isEqualTo("Hello from legacy request");
    }
}
