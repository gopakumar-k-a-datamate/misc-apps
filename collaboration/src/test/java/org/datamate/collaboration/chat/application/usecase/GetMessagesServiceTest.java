package org.datamate.collaboration.chat.application.usecase;

import org.datamate.collaboration.chat.application.dto.GetMessagesQuery;
import org.datamate.collaboration.chat.application.dto.MessageDto;
import org.datamate.collaboration.chat.application.mapper.MessageMapper;
import org.datamate.collaboration.chat.application.port.out.AttachmentRepositoryPort;
import org.datamate.collaboration.chat.application.port.out.MessageRepositoryPort;
import org.datamate.collaboration.chat.domain.model.Message;
import com.datamate.bedrock.framework.common.pagination.PageQuery;
import com.datamate.bedrock.framework.common.pagination.Paged;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetMessagesService")
class GetMessagesServiceTest {

    @Mock
    private MessageRepositoryPort messageRepository;

    @Mock
    private AttachmentRepositoryPort attachmentRepository;

    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private GetMessagesService getMessagesService;

    private UUID threadId;

    @BeforeEach
    void setUp() {
        threadId = UUID.randomUUID();
    }

    @Test
    @DisplayName("should return paginated mapped message DTOs")
    void shouldReturnPaginatedMessages() {
        PageQuery query = new PageQuery(1, 20);
        String senderId = "user-123";
        Message message = Message.restore(
                UUID.randomUUID(),
                threadId,
                senderId,
                "Hello",
                false,
                false,
                List.of(), null, Instant.now());
        Paged<Message> expectedPage = new Paged<>(List.of(message), 1, 20, 1, 1, false, false);
        MessageDto expectedDto = new MessageDto(message.getId(), message.getParentId(), message.getSenderId(), message.getText(), message.isFile(), message.isSystemMessage(), null, message.getTimestamp());

        when(messageRepository.findByThreadId(threadId, query)).thenReturn(expectedPage);
        when(messageMapper.toDto(eq(message), any(List.class))).thenReturn(expectedDto);

        Paged<MessageDto> result = getMessagesService.getMessages(threadId, query);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).senderId()).isEqualTo(senderId);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("should delegate GetMessagesQuery overload to PageQuery search")
    void shouldDelegateGetMessagesQuery_ToPageQuery() {
        GetMessagesQuery query = new GetMessagesQuery(1, 20);
        PageQuery expectedPageQuery = new PageQuery(1, 20);
        String senderId = "user-123";
        Message message = Message.restore(
                UUID.randomUUID(),
                threadId,
                senderId,
                "Hello via CQRS Query",
                false,
                false,
                List.of(), null, Instant.now());
        Paged<Message> expectedPage = new Paged<>(List.of(message), 1, 20, 1, 1, false, false);
        MessageDto expectedDto = new MessageDto(message.getId(), message.getParentId(), message.getSenderId(), message.getText(), message.isFile(), message.isSystemMessage(), null, message.getTimestamp());

        when(messageRepository.findByThreadId(threadId, expectedPageQuery)).thenReturn(expectedPage);
        when(messageMapper.toDto(eq(message), any(List.class))).thenReturn(expectedDto);

        Paged<MessageDto> result = getMessagesService.getMessages(threadId, query);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).text()).isEqualTo("Hello via CQRS Query");
    }

    @Test
    @DisplayName("should fetch and map all attachments when message has multiple attachments (Bug 8122)")
    void shouldFetchAndMapAllAttachments_WhenMessageHasMultipleAttachments() {
        PageQuery query = new PageQuery(1, 20);
        String senderId = "user-123";
        UUID attId1 = UUID.randomUUID();
        UUID attId2 = UUID.randomUUID();
        List<UUID> attachmentIds = List.of(attId1, attId2);

        Message message = Message.restore(
                UUID.randomUUID(),
                threadId,
                senderId,
                "Multiple files message",
                true,
                false,
                attachmentIds,
                null,
                Instant.now()
        );

        org.datamate.collaboration.chat.domain.model.Attachment att1 = org.datamate.collaboration.chat.domain.model.Attachment.create(
                "file1.png", "image/png", 1024L, "http://minio/file1.png", null
        );
        org.datamate.collaboration.chat.domain.model.Attachment att2 = org.datamate.collaboration.chat.domain.model.Attachment.create(
                "file2.png", "image/png", 2048L, "http://minio/file2.png", null
        );
        // Ensure IDs match
        org.springframework.test.util.ReflectionTestUtils.setField(att1, "id", attId1);
        org.springframework.test.util.ReflectionTestUtils.setField(att2, "id", attId2);

        Paged<Message> pagedMessages = new Paged<>(List.of(message), 1, 20, 1, 1, false, false);
        when(messageRepository.findByThreadId(threadId, query)).thenReturn(pagedMessages);
        when(attachmentRepository.findAllById(eq(attachmentIds))).thenReturn(List.of(att1, att2));

        MessageDto expectedDto = new MessageDto(
                message.getId(),
                null,
                senderId,
                "Multiple files message",
                true,
                false,
                List.of(
                        new org.datamate.collaboration.chat.application.dto.AttachmentResponseDto(attId1, message.getId(), "file1.png", "file1.png", "image/png", "url1", null),
                        new org.datamate.collaboration.chat.application.dto.AttachmentResponseDto(attId2, message.getId(), "file2.png", "file2.png", "image/png", "url2", null)
                ),
                message.getTimestamp()
        );
        when(messageMapper.toDto(eq(message), eq(List.of(att1, att2)))).thenReturn(expectedDto);

        Paged<MessageDto> result = getMessagesService.getMessages(threadId, query);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).attachments()).hasSize(2);
    }

    @Test
    @DisplayName("should return empty page when no messages exist")
    void shouldReturnEmptyPage_WhenNoMessagesExist() {
        PageQuery query = new PageQuery(1, 20);
        Paged<Message> emptyPage = new Paged<>(Collections.emptyList(), 1, 20, 0, 0, false, false);

        when(messageRepository.findByThreadId(threadId, query)).thenReturn(emptyPage);

        Paged<MessageDto> result = getMessagesService.getMessages(threadId, query);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }
}

