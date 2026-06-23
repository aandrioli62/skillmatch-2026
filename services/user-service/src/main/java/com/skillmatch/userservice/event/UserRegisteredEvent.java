package com.skillmatch.userservice.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a new user completes registration.
 * Routing key: user.registered
 */
@Getter
@Builder
public class UserRegisteredEvent {

    @Builder.Default
    private final String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private final String eventType = "user.registered";

    @Builder.Default
    private final Instant timestamp = Instant.now();

    @Builder.Default
    private final String source = "user-service";

    private final Data data;

    @Getter
    @Builder
    public static class Data {
        private UUID userId;
        private String keycloakId;
        private String email;
        private String role;
    }
}
