package com.skillmatch.userservice.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when an admin validates a professional's account.
 * Routing key: user.validated
 */
@Getter
@Builder
public class UserValidatedEvent {

    @Builder.Default
    private final String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private final String eventType = "user.validated";

    @Builder.Default
    private final Instant timestamp = Instant.now();

    @Builder.Default
    private final String source = "user-service";

    private final Data data;

    @Getter
    @Builder
    public static class Data {
        private UUID userId;
        private String email;
    }
}
