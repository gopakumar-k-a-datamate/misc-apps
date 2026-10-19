package org.datamate.identity.shared.messaging;

/**
 * A generic interface for publishing messages to a message broker.
 * This decouples the application from a specific messaging implementation (e.g., RabbitMQ).
 */
public interface MessagePublisher {

    /**
     * Publishes a message to the specified exchange with the given routing key.
     *
     * @param exchange   the name of the exchange or destination
     * @param routingKey the routing key for the message
     * @param message    the message payload to send
     */
    void publish(String exchange, String routingKey, Object message);
}
