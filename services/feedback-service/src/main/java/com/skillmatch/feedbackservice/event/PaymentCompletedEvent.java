package com.skillmatch.feedbackservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mirrors the payload Payment Service publishes on the {@code payment.completed}
 * routing key. Kept as a separate, minimal copy (rather than a shared library) to
 * preserve Database-per-Service / independent-deployability boundaries.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class PaymentCompletedEvent {

    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String source;
    private Data data;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    public static class Data {
        private UUID transactionId;
        private UUID contractId;
        private UUID projectId;
        private UUID companyId;
        private UUID professionalId;
        private BigDecimal totalAmount;
        private BigDecimal commissionAmount;
        private BigDecimal netAmount;
    }
}
