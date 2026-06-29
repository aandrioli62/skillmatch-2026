package com.skillmatch.userservice.config;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
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

    // Queues consumed by this service
    public static final String FEEDBACK_AGGREGATED_QUEUE = "user-service.feedback.aggregated";

    // Routing keys this service publishes
    public static final String ROUTING_KEY_USER_REGISTERED = "user.registered";
    public static final String ROUTING_KEY_USER_VALIDATED  = "user.validated";

    // Routing keys this service subscribes to
    public static final String ROUTING_KEY_FEEDBACK_AGGREGATED = "feedback.aggregated";

    @Bean
    public TopicExchange skillmatchExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue feedbackAggregatedQueue() {
        return QueueBuilder.durable(FEEDBACK_AGGREGATED_QUEUE).build();
    }

    @Bean
    public Binding feedbackAggregatedBinding(Queue feedbackAggregatedQueue, TopicExchange skillmatchExchange) {
        return BindingBuilder.bind(feedbackAggregatedQueue)
                .to(skillmatchExchange)
                .with(ROUTING_KEY_FEEDBACK_AGGREGATED);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @Primary
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jacksonMessageConverter());
        return rabbitTemplate;
    }
}
