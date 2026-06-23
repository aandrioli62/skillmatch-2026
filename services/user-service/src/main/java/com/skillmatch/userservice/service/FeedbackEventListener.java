package com.skillmatch.userservice.service;

import com.skillmatch.userservice.config.RabbitMQConfig;
import com.skillmatch.userservice.event.FeedbackSubmittedEvent;
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
     * Receives feedback.submitted events from feedback-service.
     * The event payload includes pre-aggregated avgRating and totalReviews for the reviewee,
     * allowing this service to recalculate the professional's reputation level.
     */
    @RabbitListener(queues = RabbitMQConfig.FEEDBACK_SUBMITTED_QUEUE)
    public void handleFeedbackSubmitted(FeedbackSubmittedEvent event) {
        FeedbackSubmittedEvent.Data data = event.getData();
        log.info("Received feedback.submitted event: revieweeId={}, avgRating={}, totalReviews={}",
                data.getRevieweeId(), data.getAvgRating(), data.getTotalReviews());

        userService.updateReputation(data.getRevieweeId(), data.getAvgRating(), data.getTotalReviews());
    }
}
