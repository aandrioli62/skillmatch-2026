package com.skillmatch.paymentservice.service;

import com.skillmatch.paymentservice.config.RabbitMQConfig;
import com.skillmatch.paymentservice.event.PaymentCompletedEvent;
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
     * Publishes a {@code payment.completed} event to the {@code skillmatch.events} exchange.
     * Consumed by feedback-service (enables mutual reviews) and notification-service.
     */
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        amqpTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_PAYMENT_COMPLETED,
                event);
        log.info("Published payment.completed event: transactionId={}, contractId={}",
                event.getData().getTransactionId(), event.getData().getContractId());
    }
}
