package com.skillmatch.projectservice.exception;

/**
 * Thrown when the User Service cannot be reached (or its circuit breaker is open)
 * while resolving the caller's identity or verifying a professional's status.
 * Fails closed: the operation is rejected rather than assuming success.
 */
public class UserServiceUnavailableException extends RuntimeException {

    public UserServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
