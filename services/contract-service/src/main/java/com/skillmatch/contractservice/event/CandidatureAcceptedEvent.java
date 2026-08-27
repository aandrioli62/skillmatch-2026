package com.skillmatch.contractservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mirrors the payload Project Service publishes on the {@code candidature.accepted}
 * routing key. Kept as a separate, minimal copy (rather than a shared library) to
 * preserve Database-per-Service / independent-deployability boundaries.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class CandidatureAcceptedEvent {

    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String source;
    private Data data;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    public static class Data {
        private UUID candidatureId;
        private UUID projectId;
        private UUID professionalId;
        private UUID companyId;
        private BigDecimal amount;
    }
}
