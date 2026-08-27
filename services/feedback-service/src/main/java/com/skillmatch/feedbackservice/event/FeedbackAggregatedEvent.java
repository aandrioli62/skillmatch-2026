package com.skillmatch.feedbackservice.event;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published once ratings have been aggregated for a professional. Routing key:
 * feedback.aggregated. Consumed by user-service to recalculate the professional's
 * reputation level — the field names/types here must match user-service's
 * FeedbackAggregatedEvent.Data exactly (professionalId, avgRating, totalReviews).
 */
@Getter
@Builder
public class FeedbackAggregatedEvent {

    @Builder.Default
    private final String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private final String eventType = "feedback.aggregated";

    @Builder.Default
    private final Instant timestamp = Instant.now();

    @Builder.Default
    private final String source = "feedback-service";

    private final Data data;

    @Getter
    @Builder
    public static class Data {
        private UUID professionalId;
        private BigDecimal avgRating;
        private Integer totalReviews;
    }
}
