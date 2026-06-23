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
public class UserEventPublisher {

    private final AmqpTemplate amqpTemplate;

    public void publishUserRegistered(UserRegisteredEvent event) {
        amqpTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_USER_REGISTERED,
                event);
        log.info("Published user.registered event: userId={}, email={}",
                event.getData().getUserId(), event.getData().getEmail());
    }

    public void publishUserValidated(UserValidatedEvent event) {
        amqpTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_USER_VALIDATED,
                event);
        log.info("Published user.validated event: userId={}",
                event.getData().getUserId());
    }
}
