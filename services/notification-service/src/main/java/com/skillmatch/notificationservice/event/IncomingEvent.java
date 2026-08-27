package com.skillmatch.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

/**
 * Generic shape shared by every event published on the skillmatch.events exchange
 * (eventId, eventType, timestamp, source, data). Unlike other services — which each
 * consume one or two specific event types and shadow their exact payload — Notification
 * Service subscribes to everything ("#") and only needs to read a handful of well-known
 * keys out of the data map per eventType, so a single generic class covers all of them.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class IncomingEvent {

    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String source;
    private Map<String, Object> data;
}
