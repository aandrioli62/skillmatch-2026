package com.skillmatch.notificationservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Minimal shadow of User Service's UserResponse — just the fields this service needs
 * (resolving the authenticated caller's platform-wide user id, and looking up a
 * notification recipient's email for the events that also send a real email). Kept
 * separate from User Service's own DTOs to preserve Database-per-Service independence.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class UserStatusResponse {

    private UUID id;
    private String role;
    private String status;
    private String email;
}
