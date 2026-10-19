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
                null, null, Instant.now());
        Paged<Message> expectedPage = new Paged<>(List.of(message), 1, 20, 1, 1, false, false);
        MessageDto expectedDto = new MessageDto(message.getId(), message.getParentId(), message.getSenderId(), message.getText(), message.isFile(), message.isSystemMessage(), null, message.getTimestamp());

        when(messageRepository.findByThreadId(threadId, query)).thenReturn(expectedPage);
        when(messageMapper.toDto(eq(message), any())).thenReturn(expectedDto);

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
                null, null, Instant.now());
        Paged<Message> expectedPage = new Paged<>(List.of(message), 1, 20, 1, 1, false, false);
        MessageDto expectedDto = new MessageDto(message.getId(), message.getParentId(), message.getSenderId(), message.getText(), message.isFile(), message.isSystemMessage(), null, message.getTimestamp());

        when(messageRepository.findByThreadId(threadId, expectedPageQuery)).thenReturn(expectedPage);
        when(messageMapper.toDto(eq(message), any())).thenReturn(expectedDto);

        Paged<MessageDto> result = getMessagesService.getMessages(threadId, query);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).text()).isEqualTo("Hello via CQRS Query");
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

