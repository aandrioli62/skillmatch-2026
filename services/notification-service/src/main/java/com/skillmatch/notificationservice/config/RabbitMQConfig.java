package com.skillmatch.notificationservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "skillmatch.events";

    /** Every event on the exchange ("#") — this service is a catch-all notification sink. */
    public static final String QUEUE_ALL_EVENTS = "notification.all-events";

    @Bean
    public TopicExchange skillmatchExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue allEventsQueue() {
        return new Queue(QUEUE_ALL_EVENTS, true);
    }

    @Bean
    public Binding allEventsBinding(Queue allEventsQueue, TopicExchange skillmatchExchange) {
        return BindingBuilder.bind(allEventsQueue)
                .to(skillmatchExchange)
                .with("#");
    }

    /**
     * Each publisher stamps __TypeId__ with its own fully-qualified class name, which does
     * not exist on this service's classpath. Converting inbound messages to the
     * @RabbitListener method's declared parameter type — rather than trusting that header
     * — is the standard fix for cross-service JSON events.
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}
