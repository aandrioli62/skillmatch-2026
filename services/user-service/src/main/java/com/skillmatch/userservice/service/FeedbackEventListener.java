package com.skillmatch.userservice.service;

import com.skillmatch.userservice.config.RabbitMQConfig;
import com.skillmatch.userservice.event.FeedbackAggregatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeedbackEventListener {

    private final UserService userService;

    /**
     * Receives feedback.aggregated events from feedback-service.
     * The payload includes the up-to-date avgRating and totalReviews for the professional,
     * allowing this service to recalculate the reputation level (JUNIOR / AFFIDABILE / TOP_PERFORMER).
     */
    @RabbitListener(queues = RabbitMQConfig.FEEDBACK_AGGREGATED_QUEUE)
    public void handleFeedbackAggregated(FeedbackAggregatedEvent event) {
        FeedbackAggregatedEvent.Data data = event.getData();
        log.info("Received feedback.aggregated event: professionalId={}, avgRating={}, totalReviews={}",
                data.getProfessionalId(), data.getAvgRating(), data.getTotalReviews());

        userService.updateReputation(data.getProfessionalId(), data.getAvgRating(), data.getTotalReviews());
    }
}
