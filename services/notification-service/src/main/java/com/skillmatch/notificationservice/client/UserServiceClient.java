package com.skillmatch.notificationservice.client;

import com.skillmatch.notificationservice.exception.UserServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * Resolves the internal user id of the currently authenticated caller by relaying
 * their bearer token to User Service's {@code GET /api/v1/users/me}, rather than
 * provisioning a separate service-account client, since both services sit behind
 * the same trusted network / gateway.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final RestClient userServiceRestClient;

    @CircuitBreaker(name = "default", fallbackMethod = "getCurrentUserFallback")
    public UserStatusResponse getCurrentUser() {
        return userServiceRestClient.get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + currentBearerToken())
                .retrieve()
                .body(UserStatusResponse.class);
    }

    @SuppressWarnings("unused")
    private UserStatusResponse getCurrentUserFallback(Throwable ex) {
        log.error("User Service unavailable while resolving current user: {}", ex.getMessage());
        throw new UserServiceUnavailableException(
                "Unable to resolve the authenticated caller's identity", ex);
    }

    public UUID resolveCurrentUserId() {
        return getCurrentUser().getId();
    }

    private String currentBearerToken() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getTokenValue();
        }
        throw new IllegalStateException("No authenticated JWT found in security context to relay to User Service");
    }
}
