package org.datamate.collaboration.chat.adapter.in.messaging;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import lombok.RequiredArgsConstructor;
import org.datamate.collaboration.chat.adapter.in.messaging.dto.ChatSystemEventPayload;
import org.datamate.collaboration.chat.application.dto.RecordSystemEventCommand;
import org.datamate.collaboration.chat.application.port.in.RecordSystemEventUseCase;
import org.datamate.collaboration.exception.ApplicationValidationException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Incoming Messaging Adapter (Consumer) for background status events and audit logs.
 * <p>
 * Listens to the enterprise message broker (RabbitMQ) for incoming system events,
 * translates external JSON payloads to internal commands (Anti-Corruption Layer),
 * and delegates to {@link RecordSystemEventUseCase} to persist system messages
 * and broadcast them to active WebSocket subscribers.
 */
@Component
@RequiredArgsConstructor
public class SystemEventRabbitListener {

    @EnableLogger
    private Logger logger;

    private final RecordSystemEventUseCase recordSystemEventUseCase;

    @RabbitListener(
            queues = "${collaboration.messaging.system-events.queue:chat.system.events.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void onSystemEvent(ChatSystemEventPayload payload) {
        if (payload == null) {
            if (logger != null) {
                logger.warn("Received null system event payload from broker, discarding.");
            }
            return;
        }

        if (logger != null) {
            logger.info("Received background system event from [{}] for thread [{}] with eventType [{}]",
                    payload.sourceService(), payload.threadId(), payload.eventType());
        }

        try {
            RecordSystemEventCommand command = payload.toCommand();
            recordSystemEventUseCase.recordSystemEvent(command);
        } catch (ApplicationValidationException ex) {
            if (logger != null) {
                logger.warn("Discarding invalid system event for thread [{}]: {}", payload.threadId(), ex.getMessage());
            }
        } catch (Exception ex) {
            if (logger != null) {
                logger.error("Failed to process system event for thread [{}]: {}", payload.threadId(), ex.getMessage(), ex);
            }
            throw ex;
        }
    }
}
