package com.skillmatch.userservice.service;

import com.skillmatch.userservice.config.RabbitMQConfig;
import com.skillmatch.userservice.event.UserRegisteredEvent;
import com.skillmatch.userservice.event.UserValidatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    private final AmqpTemplate amqpTemplate;

    /**
     * Publishes a {@code user.registered} event to the {@code skillmatch.events} exchange.
     * Consumed by notification-service and any other interested subscribers.
     */
    public void publishUserRegistered(UserRegisteredEvent event) {
        amqpTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_USER_REGISTERED,
                event);
        log.info("Published user.registered event: userId={}, email={}",
                event.getData().getUserId(), event.getData().getEmail());
    }

    /**
     * Publishes a {@code user.validated} event to the {@code skillmatch.events} exchange.
     * Signals that an admin has validated the professional's account.
     */
    public void publishUserValidated(UserValidatedEvent event) {
        amqpTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_USER_VALIDATED,
                event);
        log.info("Published user.validated event: userId={}",
                event.getData().getUserId());
    }
}
