package com.skillmatch.projectservice.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event published when a company publishes a project (DRAFT -> OPEN).
 * Routing key: project.published
 */
@Getter
@Builder
public class ProjectPublishedEvent {

    @Builder.Default
    private final String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private final String eventType = "project.published";

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
        private String title;
        private List<String> requiredSkills;
    }
}
