package com.skillmatch.projectservice.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a company accepts a candidature for a project.
 * Routing key: candidature.accepted
 */
@Getter
@Builder
public class CandidatureAcceptedEvent {

    @Builder.Default
    private final String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private final String eventType = "candidature.accepted";

    @Builder.Default
    private final Instant timestamp = Instant.now();

    @Builder.Default
    private final String source = "project-service";

    private final Data data;

    @Getter
    @Builder
    public static class Data {
        private UUID candidatureId;
        private UUID projectId;
        private UUID professionalId;
        private UUID companyId;
    }
}
