package org.datamate.collaboration.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for the Collaboration Microservice.
 * <p>
 * Configures the message converter (Jackson JSON), listener container factory,
 * and declares the durable exchange, queue, and binding for background system events.
 */
@Configuration
public class RabbitMqConfig {

    @Value("${collaboration.messaging.system-events.exchange:chat.system.events}")
    private String exchangeName;

    @Value("${collaboration.messaging.system-events.queue:chat.system.events.queue}")
    private String queueName;

    @Value("${collaboration.messaging.system-events.routing-key:chat.event.*}")
    private String routingKey;

    @Bean
    public TopicExchange systemEventsExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue systemEventsQueue() {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Binding systemEventsBinding(Queue systemEventsQueue, TopicExchange systemEventsExchange) {
        return BindingBuilder.bind(systemEventsQueue).to(systemEventsExchange).with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Value("${spring.rabbitmq.listener.simple.auto-startup:true}")
    private boolean autoStartup;

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAutoStartup(autoStartup);
        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
