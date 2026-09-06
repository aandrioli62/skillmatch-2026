package com.skillmatch.projectservice.client;

import com.skillmatch.projectservice.exception.UserServiceUnavailableException;
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
 * Reads a professional's role/status from User Service. Relays the current caller's
 * bearer token (the professional applying is already authenticated) rather than
 * provisioning a separate service-account client, since both services sit behind
 * the same trusted network / gateway.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final RestClient userServiceRestClient;

    @CircuitBreaker(name = "default", fallbackMethod = "getUserStatusFallback")
    public UserStatusResponse getUserStatus(UUID userId) {
        return userServiceRestClient.get()
                .uri("/api/v1/users/{userId}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + currentBearerToken())
                .retrieve()
                .body(UserStatusResponse.class);
    }

    @SuppressWarnings("unused")
    private UserStatusResponse getUserStatusFallback(UUID userId, Throwable ex) {
        log.error("User Service unavailable while checking status for userId={}: {}", userId, ex.getMessage());
        throw new UserServiceUnavailableException(
                "Unable to verify status for user id=" + userId, ex);
    }

    /**
     * Resolves the internal user record for the currently authenticated caller, translating
     * the relayed JWT into the platform-wide user id via User Service's {@code GET /api/v1/users/me}.
     * Annotated directly on this method (rather than delegating to a separate, internally called
     * getCurrentUser()) because Spring AOP proxies self-invocations right past the @CircuitBreaker
     * advice — a delegating call would silently skip the fallback.
     */
    @CircuitBreaker(name = "default", fallbackMethod = "resolveCurrentUserIdFallback")
    public UUID resolveCurrentUserId() {
        return userServiceRestClient.get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + currentBearerToken())
                .retrieve()
                .body(UserStatusResponse.class)
                .getId();
    }

    @SuppressWarnings("unused")
    private UUID resolveCurrentUserIdFallback(Throwable ex) {
        log.error("User Service unavailable while resolving current user: {}", ex.getMessage());
        throw new UserServiceUnavailableException(
                "Unable to resolve the authenticated caller's identity", ex);
    }

    private String currentBearerToken() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getTokenValue();
        }
        throw new IllegalStateException("No authenticated JWT found in security context to relay to User Service");
    }
}
