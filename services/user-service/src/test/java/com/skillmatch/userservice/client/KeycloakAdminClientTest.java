package com.skillmatch.userservice.client;

import com.skillmatch.userservice.exception.KeycloakAdminException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

@DisplayName("KeycloakAdminClient — Unit Tests")
class KeycloakAdminClientTest {

    private static final String BASE_URL = "http://keycloak";
    private static final String TOKEN_URI = BASE_URL + "/realms/master/protocol/openid-connect/token";
    private static final String CREATE_USER_URI = BASE_URL + "/admin/realms/skillmatch/users";
    private static final String ROLE_URI = BASE_URL + "/admin/realms/skillmatch/roles/PROFESSIONAL";

    private MockRestServiceServer mockServer;
    private KeycloakAdminClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        client = new KeycloakAdminClient(builder.build(), "master", "admin", "admin", "skillmatch");
    }

    private void expectToken() {
        mockServer.expect(requestTo(TOKEN_URI))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"access_token\":\"admin-token\"}", MediaType.APPLICATION_JSON));
    }

    private void expectCreateUser(String keycloakId) {
        mockServer.expect(requestTo(CREATE_USER_URI))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer admin-token"))
                .andRespond(withCreatedEntity(URI.create(CREATE_USER_URI + "/" + keycloakId)));
    }

    private void expectRoleLookupAndAssignment(String keycloakId) {
        mockServer.expect(requestTo(ROLE_URI))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"id\":\"role-id-1\",\"name\":\"PROFESSIONAL\"}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(BASE_URL + "/admin/realms/skillmatch/users/" + keycloakId + "/role-mappings/realm"))
                .andExpect(method(POST))
                .andRespond(withNoContent());
    }

    @Test
    @DisplayName("createUserWithRole: obtains a token, creates the user, assigns the role, returns the new id")
    void createUserWithRole_success() {
        String keycloakId = "kc-new-1";
        expectToken();
        expectCreateUser(keycloakId);
        expectRoleLookupAndAssignment(keycloakId);

        String result = client.createUserWithRole("pro@example.com", "s3cr3t!!", "PROFESSIONAL",
                "Mario", "Rossi");

        assertThat(result).isEqualTo(keycloakId);
        mockServer.verify();
    }

    @Test
    @DisplayName("token endpoint failure raises KeycloakAdminException")
    void createUserWithRole_tokenFailure_throwsKeycloakAdminException() {
        mockServer.expect(requestTo(TOKEN_URI)).andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> client.createUserWithRole("pro@example.com", "pw", "PROFESSIONAL", "Mario", "Rossi"))
                .isInstanceOf(KeycloakAdminException.class);
    }

    @Test
    @DisplayName("missing access_token in the token response raises KeycloakAdminException")
    void createUserWithRole_missingAccessToken_throwsKeycloakAdminException() {
        mockServer.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.createUserWithRole("pro@example.com", "pw", "PROFESSIONAL", "Mario", "Rossi"))
                .isInstanceOf(KeycloakAdminException.class)
                .hasMessageContaining("access_token");
    }

    @Test
    @DisplayName("Keycloak rejecting the new user (e.g. duplicate) raises KeycloakAdminException")
    void createUserWithRole_createUserFails_throwsKeycloakAdminException() {
        expectToken();
        mockServer.expect(requestTo(CREATE_USER_URI)).andRespond(withServerError());

        assertThatThrownBy(() -> client.createUserWithRole("pro@example.com", "pw", "PROFESSIONAL", "Mario", "Rossi"))
                .isInstanceOf(KeycloakAdminException.class);
    }

    @Test
    @DisplayName("missing Location header on user creation raises KeycloakAdminException")
    void createUserWithRole_noLocationHeader_throwsKeycloakAdminException() {
        expectToken();
        mockServer.expect(requestTo(CREATE_USER_URI))
                .andRespond(withSuccess());

        assertThatThrownBy(() -> client.createUserWithRole("pro@example.com", "pw", "PROFESSIONAL", "Mario", "Rossi"))
                .isInstanceOf(KeycloakAdminException.class)
                .hasMessageContaining("Location");
    }

    @Test
    @DisplayName("role lookup failure raises KeycloakAdminException")
    void createUserWithRole_roleLookupFails_throwsKeycloakAdminException() {
        String keycloakId = "kc-new-2";
        expectToken();
        expectCreateUser(keycloakId);
        mockServer.expect(requestTo(ROLE_URI)).andRespond(withServerError());

        assertThatThrownBy(() -> client.createUserWithRole("pro@example.com", "pw", "PROFESSIONAL", "Mario", "Rossi"))
                .isInstanceOf(KeycloakAdminException.class);
    }
}
