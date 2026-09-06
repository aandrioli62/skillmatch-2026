package com.skillmatch.userservice.model.enums;

public enum UserStatus {
    PENDING,
    VALIDATED,
    SUSPENDED,
    // Admin-initiated account removal. Unlike SUSPENDED (a reversible, temporary hold
    // that still leaves the Keycloak identity enabled), DEACTIVATED also disables the
    // Keycloak account — the user can never log in again — while the row itself is kept
    // so contracts/payments/feedback already tied to this id stay resolvable for admins.
    DEACTIVATED
}
