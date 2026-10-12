package org.datamate.collaboration.chat.application.usecase;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import lombok.RequiredArgsConstructor;
import org.datamate.collaboration.chat.application.dto.MessageDto;
import org.datamate.collaboration.chat.application.dto.RecordSystemEventCommand;
import org.datamate.collaboration.chat.application.mapper.MessageMapper;
import org.datamate.collaboration.chat.application.port.in.RecordSystemEventUseCase;
import org.datamate.collaboration.chat.application.port.out.MessageBroadcastPort;
import org.datamate.collaboration.chat.application.port.out.MessageRepositoryPort;
import org.datamate.collaboration.chat.application.port.out.ThreadRepositoryPort;
import org.datamate.collaboration.chat.domain.model.Message;
import org.datamate.collaboration.chat.domain.model.Thread;
import org.datamate.collaboration.exception.ApplicationValidationException;
import org.datamate.collaboration.exception.CollaborationErrorCodes;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application Service for recording background status events and automated audit logs.
 * <p>
 * Implements {@link RecordSystemEventUseCase}. Handles the command side of automated
 * system events. Persists the event as an immutable, read-only system message
 * (isSystemMessage = true) and broadcasts it to connected WebSocket clients.
 */
@Service
@RequiredArgsConstructor
public class RecordSystemEventService implements RecordSystemEventUseCase {

    @EnableLogger
    private Logger logger;

    private final MessageRepositoryPort messageRepository;
    private final ThreadRepositoryPort threadRepository;
    private final MessageBroadcastPort messageBroadcastPort;
    private final MessageMapper messageMapper;

    @Override
    @Transactional
    public MessageDto recordSystemEvent(RecordSystemEventCommand command) {
        validateCommand(command);
        ensureThreadExists(command.threadId());

        String senderId = (command.senderId() != null && !command.senderId().isBlank())
                ? command.senderId().trim()
                : "SYSTEM";

        Message message = Message.createSystemMessage(
                command.threadId(),
                senderId,
                command.text().trim(),
                command.parentId()
        );

        Message savedMessage = messageRepository.save(message);

        MessageDto messageDto = messageMapper.toDto(savedMessage != null ? savedMessage : message, null);

        if (logger != null) {
            logger.info("Recorded system event message [{}] for thread [{}] from sender [{}]",
                    messageDto.id(), command.threadId(), senderId);
        }

        messageBroadcastPort.broadcastMessage(command.threadId(), messageDto);

        return messageDto;
    }

    private void validateCommand(RecordSystemEventCommand command) {
        if (command == null) {
            throw new ApplicationValidationException(
                    CollaborationErrorCodes.REQUIRED_FIELD_MISSING.code(), "command");
        }
        if (command.threadId() == null) {
            throw new ApplicationValidationException(
                    CollaborationErrorCodes.REQUIRED_FIELD_MISSING.code(), "threadId");
        }
        if (command.text() == null || command.text().isBlank()) {
            throw new ApplicationValidationException(
                    CollaborationErrorCodes.REQUIRED_FIELD_MISSING.code(), "text");
        }
    }

    private void ensureThreadExists(UUID threadId) {
        if (!threadRepository.existsById(threadId)) {
            try {
                threadRepository.save(new Thread(threadId));
                if (logger != null) {
                    logger.info("Lazily created thread [{}] for incoming system event", threadId);
                }
            } catch (DataIntegrityViolationException e) {
                if (logger != null) {
                    logger.debug("Thread [{}] was concurrently created by another transaction, proceeding.", threadId);
                }
            }
        }
    }
}
