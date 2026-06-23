package com.skillmatch.userservice.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event received from feedback-service when a new feedback is submitted.
 * Routing key: feedback.submitted
 * Contains pre-aggregated reputation stats for the reviewee (professional).
 */
@Getter
@Setter
@NoArgsConstructor
public class FeedbackSubmittedEvent {

    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String source;
    private Data data;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Data {
        private UUID revieweeId;
        private BigDecimal avgRating;
        private Integer totalReviews;
    }
}
