package com.skillmatch.userservice.client;

import com.skillmatch.userservice.exception.KeycloakAdminException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Creates Keycloak accounts for self-service registration via the Admin REST API,
 * authenticating as the realm's bootstrap admin user (same credentials already used
 * to stand up Keycloak itself — see infra/k8s/config.yaml, keycloak-admin-credentials).
 * A dedicated service-account client was deliberately skipped for this project's scope.
 */
@Component
public class KeycloakAdminClient {

    private final RestClient restClient;
    private final String adminRealm;
    private final String adminUsername;
    private final String adminPassword;
    private final String targetRealm;

    public KeycloakAdminClient(
            RestClient keycloakAdminRestClient,
            @Value("${keycloak.admin.admin-realm}") String adminRealm,
            @Value("${keycloak.admin.username}") String adminUsername,
            @Value("${keycloak.admin.password}") String adminPassword,
            @Value("${keycloak.admin.realm}") String targetRealm) {
        this.restClient = keycloakAdminRestClient;
        this.adminRealm = adminRealm;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.targetRealm = targetRealm;
    }

    /**
     * Creates a new user in the target realm with a permanent password and returns
     * its Keycloak id, then assigns the given realm role.
     *
     * @throws KeycloakAdminException if Keycloak rejects the request (e.g. username/email already taken)
     */
    public String createUserWithRole(String email, String password, String realmRole,
                                      String firstName, String lastName) {
        String token = obtainAdminToken();
        String keycloakId = createUser(token, email, password, firstName, lastName);
        assignRealmRole(token, keycloakId, realmRole);
        return keycloakId;
    }

    private String obtainAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", adminUsername);
        form.add("password", adminPassword);

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", adminRealm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            Object accessToken = response != null ? response.get("access_token") : null;
            if (accessToken == null) {
                throw new KeycloakAdminException("Keycloak token response did not contain an access_token");
            }
            return accessToken.toString();
        } catch (RestClientException ex) {
            throw new KeycloakAdminException("Failed to obtain a Keycloak admin token", ex);
        }
    }

    private String createUser(String token, String email, String password, String firstName, String lastName) {
        // firstName/lastName are required by Keycloak's default declarative user
        // profile; omitting them triggers a VERIFY_PROFILE required action on the
        // user's very first login, blocking the redirect back into the app.
        Map<String, Object> body = Map.of(
                "username", email,
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "enabled", true,
                "emailVerified", true,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", password,
                        "temporary", false
                ))
        );

        try {
            URI location = restClient.post()
                    .uri("/admin/realms/{realm}/users", targetRealm)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity()
                    .getHeaders()
                    .getLocation();
            if (location == null) {
                throw new KeycloakAdminException("Keycloak did not return a Location header for the created user");
            }
            String path = location.getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        } catch (RestClientException ex) {
            throw new KeycloakAdminException("Failed to create the Keycloak user for email " + email, ex);
        }
    }

    /**
     * Disables the Keycloak account so it can never log in again — used when an admin
     * deactivates a user. The user record itself is kept in this service's own database
     * (see UserServiceImpl.deactivateUser), only the identity is locked out.
     *
     * @throws KeycloakAdminException if Keycloak rejects the request (e.g. unknown id)
     */
    public void disableUser(String keycloakId) {
        String token = obtainAdminToken();
        try {
            restClient.put()
                    .uri("/admin/realms/{realm}/users/{id}", targetRealm, keycloakId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("enabled", false))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new KeycloakAdminException("Failed to disable the Keycloak user " + keycloakId, ex);
        }
    }

    private void assignRealmRole(String token, String keycloakId, String roleName) {
        try {
            Map<String, Object> role = restClient.get()
                    .uri("/admin/realms/{realm}/roles/{roleName}", targetRealm, roleName)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            restClient.post()
                    .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", targetRealm, keycloakId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(role))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new KeycloakAdminException(
                    "Failed to assign realm role " + roleName + " to Keycloak user " + keycloakId, ex);
        }
    }
}
