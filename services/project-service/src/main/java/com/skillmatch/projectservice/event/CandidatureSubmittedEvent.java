package com.skillmatch.projectservice.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a professional applies to an open project.
 * Routing key: candidature.submitted
 */
@Getter
@Builder
public class CandidatureSubmittedEvent {

    @Builder.Default
    private final String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private final String eventType = "candidature.submitted";

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
        private String projectTitle;
        private UUID professionalId;
        private UUID companyId;
    }
}
