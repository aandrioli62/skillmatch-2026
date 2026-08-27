package com.skillmatch.notificationservice.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class NotificationResponse {

    private String id;
    private String eventType;
    private UUID recipientId;
    private String message;
    private boolean read;
    private Instant createdAt;
}
