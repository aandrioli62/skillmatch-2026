package com.skillmatch.feedbackservice.service;

import com.skillmatch.feedbackservice.config.RabbitMQConfig;
import com.skillmatch.feedbackservice.event.FeedbackAggregatedEvent;
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
     * Publishes a {@code feedback.aggregated} event to the {@code skillmatch.events} exchange.
     * Consumed by user-service to recalculate the professional's reputation level.
     */
    public void publishFeedbackAggregated(FeedbackAggregatedEvent event) {
        amqpTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_FEEDBACK_AGGREGATED,
                event);
        log.info("Published feedback.aggregated event: professionalId={}, avgRating={}, totalReviews={}",
                event.getData().getProfessionalId(), event.getData().getAvgRating(), event.getData().getTotalReviews());
    }
}
