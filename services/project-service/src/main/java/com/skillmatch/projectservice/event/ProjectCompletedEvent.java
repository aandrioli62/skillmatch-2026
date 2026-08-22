package com.skillmatch.projectservice.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a company marks a project as completed.
 * Routing key: project.completed
 */
@Getter
@Builder
public class ProjectCompletedEvent {

    @Builder.Default
    private final String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private final String eventType = "project.completed";

    @Builder.Default
    private final Instant timestamp = Instant.now();

    @Builder.Default
    private final String source = "project-service";

    private final Data data;

    @Getter
    @Builder
    public static class Data {
        private UUID projectId;
        private UUID companyId;
        private UUID professionalId;
    }
}
