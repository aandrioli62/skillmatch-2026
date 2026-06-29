package com.skillmatch.userservice.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event received from feedback-service after ratings have been aggregated for a professional.
 * Routing key: feedback.aggregated
 * Published by feedback-service once it has computed the updated avg_rating and total_reviews
 * for a reviewee, so user-service can recalculate the reputation level accordingly.
 */
@Getter
@Setter
@NoArgsConstructor
public class FeedbackAggregatedEvent {

    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String source;
    private Data data;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Data {
        /** UUID of the professional whose reputation must be recalculated. */
        private UUID professionalId;
        /** Updated average rating across all received feedbacks. */
        private BigDecimal avgRating;
        /** Total number of reviews received so far. */
        private Integer totalReviews;
    }
}
