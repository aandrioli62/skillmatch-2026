package com.skillmatch.paymentservice.exception;

/**
 * Thrown when User Service cannot be reached (or its circuit breaker is open)
 * while resolving the caller's identity. Fails closed: the operation is rejected
 * rather than assuming success.
 */
public class UserServiceUnavailableException extends RuntimeException {

    public UserServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
