package com.skillmatch.projectservice.service;

import com.skillmatch.projectservice.config.RabbitMQConfig;
import com.skillmatch.projectservice.event.CandidatureAcceptedEvent;
import com.skillmatch.projectservice.event.ProjectCompletedEvent;
import com.skillmatch.projectservice.event.ProjectPublishedEvent;
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
     * Publishes a {@code project.published} event to the {@code skillmatch.events} exchange.
     * Consumed by notification-service to alert matching professionals.
     */
    public void publishProjectPublished(ProjectPublishedEvent event) {
        amqpTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_PROJECT_PUBLISHED,
                event);
        log.info("Published project.published event: projectId={}", event.getData().getProjectId());
    }

    /**
     * Publishes a {@code candidature.accepted} event to the {@code skillmatch.events} exchange.
     * Consumed by contract-service (creates the micro-contract) and notification-service.
     */
    public void publishCandidatureAccepted(CandidatureAcceptedEvent event) {
        amqpTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_CANDIDATURE_ACCEPTED,
                event);
        log.info("Published candidature.accepted event: candidatureId={}, projectId={}",
                event.getData().getCandidatureId(), event.getData().getProjectId());
    }

    /**
     * Publishes a {@code project.completed} event to the {@code skillmatch.events} exchange.
     * Consumed by payment-service (enables payment) and contract-service.
     */
    public void publishProjectCompleted(ProjectCompletedEvent event) {
        amqpTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_PROJECT_COMPLETED,
                event);
        log.info("Published project.completed event: projectId={}", event.getData().getProjectId());
    }
}
