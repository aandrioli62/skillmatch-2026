package com.skillmatch.projectservice.client;

import com.skillmatch.projectservice.exception.UserServiceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The {@code @CircuitBreaker} annotation is Resilience4j AOP: it only intercepts calls when
 * the bean is proxied inside a Spring context, so a plain unit test (no context) exercises
 * {@link UserServiceClient}'s HTTP logic directly, and the fallback methods are invoked
 * explicitly via {@link ReflectionTestUtils} rather than by triggering a real circuit trip.
 */
class UserServiceClientTest {

    private MockRestServiceServer mockServer;
    private UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://user-service");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        userServiceClient = new UserServiceClient(builder.build());
        authenticateAs("token-abc");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String tokenValue) {
        Jwt jwt = Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .claim("sub", "kc-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    void getUserStatus_relaysBearerTokenAndParsesResponse() {
        UUID userId = UUID.randomUUID();
        mockServer.expect(requestTo("http://user-service/api/v1/users/" + userId))
                .andExpect(method(GET))
                .andExpect(header("Authorization", "Bearer token-abc"))
                .andRespond(withSuccess(
                        "{\"id\":\"" + userId + "\",\"role\":\"PROFESSIONAL\",\"status\":\"VALIDATED\"}",
                        MediaType.APPLICATION_JSON));

        UserStatusResponse response = userServiceClient.getUserStatus(userId);

        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getRole()).isEqualTo("PROFESSIONAL");
        assertThat(response.getStatus()).isEqualTo("VALIDATED");
        mockServer.verify();
    }

    @Test
    void resolveCurrentUserId_returnsIdFromMeEndpoint() {
        UUID userId = UUID.randomUUID();
        mockServer.expect(requestTo("http://user-service/api/v1/users/me"))
                .andExpect(method(GET))
                .andExpect(header("Authorization", "Bearer token-abc"))
                .andRespond(withSuccess(
                        "{\"id\":\"" + userId + "\",\"role\":\"COMPANY\",\"status\":\"VALIDATED\"}",
                        MediaType.APPLICATION_JSON));

        UUID resolved = userServiceClient.resolveCurrentUserId();

        assertThat(resolved).isEqualTo(userId);
        mockServer.verify();
    }

    @Test
    void currentBearerToken_noAuthentication_throwsIllegalState() {
        SecurityContextHolder.clearContext();
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> userServiceClient.getUserStatus(userId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getUserStatus_downstreamError_propagatesAsHttpException() {
        UUID userId = UUID.randomUUID();
        mockServer.expect(requestTo("http://user-service/api/v1/users/" + userId))
                .andRespond(withServerError());

        // No Spring context here, so the @CircuitBreaker annotation has no AOP proxy to
        // intercept this and redirect to the fallback: the raw HTTP exception surfaces,
        // exactly as it would from the RestClient call itself.
        assertThatThrownBy(() -> userServiceClient.getUserStatus(userId))
                .isNotInstanceOf(UserServiceUnavailableException.class);
    }

    @Test
    void getUserStatusFallback_wrapsThrowableAsUserServiceUnavailable() {
        UUID userId = UUID.randomUUID();
        RuntimeException cause = new RuntimeException("connection refused");

        assertThatThrownBy(() ->
                ReflectionTestUtils.invokeMethod(userServiceClient, "getUserStatusFallback", userId, cause))
                .isInstanceOf(UserServiceUnavailableException.class)
                .hasCause(cause);
    }

    @Test
    void getCurrentUserFallback_wrapsThrowableAsUserServiceUnavailable() {
        RuntimeException cause = new RuntimeException("timeout");

        assertThatThrownBy(() ->
                ReflectionTestUtils.invokeMethod(userServiceClient, "getCurrentUserFallback", cause))
                .isInstanceOf(UserServiceUnavailableException.class)
                .hasCause(cause);
    }
}
