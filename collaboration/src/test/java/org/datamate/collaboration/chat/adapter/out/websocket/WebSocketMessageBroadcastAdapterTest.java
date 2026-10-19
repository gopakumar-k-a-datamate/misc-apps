package org.datamate.collaboration.chat.adapter.out.websocket;

import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.datamate.collaboration.chat.application.dto.MessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketMessageBroadcastAdapter")
class WebSocketMessageBroadcastAdapterTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private Logger logger;

    @InjectMocks
    private WebSocketMessageBroadcastAdapter adapter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adapter, "logger", logger);
    }

    @Test
    @DisplayName("should broadcast message to specific thread WebSocket topic")
    void shouldBroadcastMessageToThreadTopic() {
        UUID threadId = UUID.randomUUID();
        MessageDto messageDto = new MessageDto(
                UUID.randomUUID(),
                null,
                "user-123",
                "Hello, STOMP!",
                false,
                false,
                null,
                Instant.now()
        );

        adapter.broadcastMessage(threadId, messageDto);

        String expectedDestination = "/topic/thread." + threadId;
        verify(messagingTemplate).convertAndSend(expectedDestination, messageDto);
    }
}



