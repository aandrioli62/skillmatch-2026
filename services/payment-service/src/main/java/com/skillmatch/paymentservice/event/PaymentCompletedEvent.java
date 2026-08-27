package com.skillmatch.paymentservice.event;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a payment has been processed for a contract.
 * Routing key: payment.completed
 */
@Getter
@Builder
public class PaymentCompletedEvent {

    @Builder.Default
    private final String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private final String eventType = "payment.completed";

    @Builder.Default
    private final Instant timestamp = Instant.now();

    @Builder.Default
    private final String source = "payment-service";

    private final Data data;

    @Getter
    @Builder
    public static class Data {
        private UUID transactionId;
        private UUID contractId;
        private UUID companyId;
        private UUID professionalId;
        private BigDecimal totalAmount;
        private BigDecimal commissionAmount;
        private BigDecimal netAmount;
    }
}
