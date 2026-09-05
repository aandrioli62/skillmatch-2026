package com.skillmatch.userservice.exception;

/**
 * Thrown when a call to the Keycloak Admin REST API fails while provisioning
 * a self-service registration (see {@link com.skillmatch.userservice.client.KeycloakAdminClient}).
 */
public class KeycloakAdminException extends RuntimeException {

    public KeycloakAdminException(String message) {
        super(message);
    }

    public KeycloakAdminException(String message, Throwable cause) {
        super(message, cause);
    }
}
