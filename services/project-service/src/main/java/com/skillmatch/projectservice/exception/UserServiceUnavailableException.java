package com.skillmatch.projectservice.exception;

import java.util.UUID;

/**
 * Thrown when the User Service cannot be reached (or its circuit breaker is open)
 * while verifying a professional's status. Fails closed: the candidature is rejected
 * rather than assuming the professional is VALIDATED.
 */
public class UserServiceUnavailableException extends RuntimeException {

    public UserServiceUnavailableException(UUID userId, Throwable cause) {
        super("Unable to verify status for user id=" + userId + ": User Service is unavailable", cause);
    }
}
