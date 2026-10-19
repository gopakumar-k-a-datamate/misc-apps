package org.datamate.pharmacy.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for the Pharmacy Service.
 *
 * <h2>Purpose</h2>
 * <p>Registers a {@link Jackson2JsonMessageConverter} so that Spring AMQP knows to
 * deserialize inbound RabbitMQ messages from JSON into Java objects (specifically
 * {@link org.datamate.pharmacy.messaging.SubjectSyncMessage}) rather than attempting
 * Java binary deserialization, which would fail.</p>
 *
 * <h2>Why This Is Required</h2>
 * <p>The Identity Service serializes its {@code SubjectSyncMessage} to JSON using Jackson
 * before publishing. Without this converter registered on the consumer side,
 * {@code @RabbitListener} methods would receive raw bytes and throw a
 * {@code MessageConversionException} on every message.</p>
 *
 * <h2>Note on Exchange Declaration</h2>
 * <p>The {@code auth.subject.sync} Fanout exchange is declared by the Identity Service
 * (the producer). The Pharmacy Service only binds to it via {@code @QueueBinding} in
 * {@link org.datamate.pharmacy.messaging.SubjectSyncRabbitListener}. No duplicate
 * exchange bean is needed here.</p>
 */
@Configuration
public class RabbitMqConfig {

    /**
     * Registers Jackson as the message converter for all AMQP operations in this service.
     * This single bean is picked up automatically by Spring AMQP for both
     * {@code @RabbitListener} deserialization and any {@code RabbitTemplate} calls.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configures the listener container factory to use the JSON converter.
     * This ensures every {@code @RabbitListener} in this service deserializes
     * inbound messages using Jackson, not Java serialization.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}
