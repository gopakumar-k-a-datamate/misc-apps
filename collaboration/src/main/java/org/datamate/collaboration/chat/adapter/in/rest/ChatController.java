package org.datamate.collaboration.chat.adapter.in.rest;

import com.datamate.bedrock.framework.common.pagination.Paged;
import com.datamate.bedrock.framework.common.pagination.PaginatedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.datamate.collaboration.chat.application.dto.GetMessagesQuery;
import org.datamate.collaboration.chat.application.dto.MessageDto;
import org.datamate.collaboration.chat.application.dto.SendMessageCommand;
import org.datamate.collaboration.chat.application.port.in.GetMessagesUseCase;
import org.datamate.collaboration.chat.application.port.in.SendMessageUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * Incoming REST Adapter for the Chat domain.
 * <p>
 * Strictly follows the context path convention:
 * <code>/api/{version}/{module_name}/...</code>
 * <ul>
 *   <li>{@code POST /api/v1/collaboration/threads/{threadId}/messages} - Submit new message (CQRS Command)</li>
 *   <li>{@code GET  /api/v1/collaboration/threads/{threadId}/messages} - Fetch message history (CQRS Query)</li>
 * </ul>
 * Authentication and thread binding are validated via the signed JWT Ticket filter.
 */
@CrossOrigin
@RestController
@RequestMapping("/api/v1/collaboration/threads/{threadId}/messages")
@RequiredArgsConstructor
public class ChatController {

    private final SendMessageUseCase sendMessageUseCase;
    private final GetMessagesUseCase getMessagesUseCase;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public void sendMessage(
            @PathVariable UUID threadId,
            @Valid @RequestBody SendMessageCommand command,
            Principal principal) {

        String senderId = (principal != null && principal.getName() != null) ? principal.getName() : "Anonymous";
        sendMessageUseCase.sendMessage(threadId, senderId, command);
    }

    @GetMapping
    public PaginatedResponse<MessageDto> getMessages(
            @PathVariable UUID threadId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        GetMessagesQuery query = new GetMessagesQuery(page, size);
        Paged<MessageDto> paged = getMessagesUseCase.getMessages(threadId, query);
        return PaginatedResponse.of(paged);
    }
}
