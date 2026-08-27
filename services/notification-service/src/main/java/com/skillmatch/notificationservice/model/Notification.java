package com.skillmatch.notificationservice.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A single in-app notification generated from a domain event. For now, "sending" a
 * notification just means logging it and persisting it here (mocking the email
 * channel) — see roadmap Fase 5.
 */
@Document(collection = "notifications")
@Getter
@Setter
public class Notification {

    @Id
    private String id;

    /** Routing key of the domain event this notification was generated from, e.g. "payment.completed". */
    private String eventType;

    /** Platform-wide user id of the recipient. */
    private UUID recipientId;

    /** Human-readable notification text. */
    private String message;

    /** Raw event payload, kept for reference/debugging. */
    private Map<String, Object> data;

    private boolean read = false;

    private Instant createdAt;
}
