package org.datamate.collaboration.chat.application.port.in;

import org.datamate.collaboration.chat.application.dto.MessageDto;
import org.datamate.collaboration.chat.application.dto.RecordSystemEventCommand;

/**
 * Inbound Port for recording background status events and automated audit actions
 * into a chat thread as read-only system messages.
 * <p>
 * Decouples the driving adapter (e.g. RabbitMQ/Kafka event consumer, internal REST trigger)
 * from the core application orchestration and domain logic.
 */
public interface RecordSystemEventUseCase {

    /**
     * Records an incoming system audit event, lazily initializes the thread if necessary,
     * persists it as a system message in the database, and broadcasts it to active WebSocket
     * subscribers on the thread's topic.
     *
     * @param command the CQRS command holding event details
     * @return the recorded MessageDto representing the persisted system message
     */
    MessageDto recordSystemEvent(RecordSystemEventCommand command);
}
