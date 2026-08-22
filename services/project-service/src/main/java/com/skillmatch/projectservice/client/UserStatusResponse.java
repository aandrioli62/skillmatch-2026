package com.skillmatch.projectservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Minimal shadow of User Service's UserResponse — only the fields Project Service
 * needs to enforce the "professional must be VALIDATED" business rule. Kept separate
 * from User Service's own DTOs to preserve Database-per-Service independence.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class UserStatusResponse {

    private UUID id;
    private String role;
    private String status;
}
