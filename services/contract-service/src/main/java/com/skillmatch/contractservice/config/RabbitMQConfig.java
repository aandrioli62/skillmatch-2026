package com.skillmatch.contractservice.config;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "skillmatch.events";

    // Routing keys this service consumes
    public static final String ROUTING_KEY_CANDIDATURE_ACCEPTED = "candidature.accepted";

    public static final String QUEUE_CANDIDATURE_ACCEPTED = "contract.candidature.accepted";

    @Bean
    public TopicExchange skillmatchExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue candidatureAcceptedQueue() {
        return new Queue(QUEUE_CANDIDATURE_ACCEPTED, true);
    }

    @Bean
    public Binding candidatureAcceptedBinding(Queue candidatureAcceptedQueue, TopicExchange skillmatchExchange) {
        return BindingBuilder.bind(candidatureAcceptedQueue)
                .to(skillmatchExchange)
                .with(ROUTING_KEY_CANDIDATURE_ACCEPTED);
    }

    /**
     * The publisher (project-service) stamps __TypeId__ with its own fully-qualified
     * class name, which does not exist on this service's classpath. Converting inbound
     * messages to the @RabbitListener method's declared parameter type — rather than
     * trusting that header — is the standard fix for cross-service JSON events.
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }

    @Bean
    @Primary
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jacksonMessageConverter());
        return rabbitTemplate;
    }
}
