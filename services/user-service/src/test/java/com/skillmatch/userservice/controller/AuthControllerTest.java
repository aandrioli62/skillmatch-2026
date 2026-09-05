package com.skillmatch.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatch.userservice.client.KeycloakAdminClient;
import com.skillmatch.userservice.config.TestSecurityConfig;
import com.skillmatch.userservice.dto.request.CompanyProfileRequest;
import com.skillmatch.userservice.dto.request.ProfessionalProfileRequest;
import com.skillmatch.userservice.dto.response.UserResponse;
import com.skillmatch.userservice.exception.DuplicateEmailException;
import com.skillmatch.userservice.exception.GlobalExceptionHandler;
import com.skillmatch.userservice.exception.KeycloakAdminException;
import com.skillmatch.userservice.model.enums.UserRole;
import com.skillmatch.userservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("AuthController — @WebMvcTest")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private KeycloakAdminClient keycloakAdminClient;

    private Map<String, Object> professionalPayload(String email) {
        return Map.of(
                "email", email,
                "password", "s3cr3t123",
                "role", "PROFESSIONAL",
                "firstName", "Mario",
                "lastName", "Rossi");
    }

    private Map<String, Object> companyPayload(String email) {
        return Map.of(
                "email", email,
                "password", "s3cr3t123",
                "role", "COMPANY",
                "companyName", "ACME S.r.l.");
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("PROFESSIONAL: provisions Keycloak, registers the user, updates the professional profile")
        void register_professional_success() throws Exception {
            UUID userId = UUID.randomUUID();
            UserResponse response = new UserResponse();
            response.setId(userId);
            response.setEmail("pro@example.com");
            response.setRole(UserRole.PROFESSIONAL);

            when(userService.emailExists("pro@example.com")).thenReturn(false);
            when(keycloakAdminClient.createUserWithRole(eq("pro@example.com"), eq("s3cr3t123"), eq("PROFESSIONAL"), eq("Mario"), eq("Rossi")))
                    .thenReturn("kc-new-1");
            when(userService.registerUser(any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(professionalPayload("pro@example.com"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("pro@example.com"));

            verify(userService).updateProfessionalProfile(eq(userId), any(ProfessionalProfileRequest.class));
            verify(userService, never()).updateCompanyProfile(any(), any());
        }

        @Test
        @DisplayName("COMPANY: provisions Keycloak, registers the user, updates the company profile")
        void register_company_success() throws Exception {
            UUID userId = UUID.randomUUID();
            UserResponse response = new UserResponse();
            response.setId(userId);
            response.setEmail("company@example.com");
            response.setRole(UserRole.COMPANY);

            when(userService.emailExists("company@example.com")).thenReturn(false);
            when(keycloakAdminClient.createUserWithRole(eq("company@example.com"), eq("s3cr3t123"), eq("COMPANY"), any(), any()))
                    .thenReturn("kc-new-2");
            when(userService.registerUser(any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(companyPayload("company@example.com"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("company@example.com"));

            verify(userService).updateCompanyProfile(eq(userId), any(CompanyProfileRequest.class));
            verify(userService, never()).updateProfessionalProfile(any(), any());
        }

        @Test
        @DisplayName("ADMIN role is rejected with 422, without contacting Keycloak")
        void register_adminRole_rejected() throws Exception {
            Map<String, Object> payload = Map.of(
                    "email", "admin2@example.com",
                    "password", "s3cr3t123",
                    "role", "ADMIN");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isUnprocessableEntity());

            verify(keycloakAdminClient, never()).createUserWithRole(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("PROFESSIONAL missing first/last name is rejected with 422")
        void register_professionalMissingName_rejected() throws Exception {
            Map<String, Object> payload = Map.of(
                    "email", "pro2@example.com",
                    "password", "s3cr3t123",
                    "role", "PROFESSIONAL");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isUnprocessableEntity());

            verify(keycloakAdminClient, never()).createUserWithRole(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("COMPANY missing company name is rejected with 422")
        void register_companyMissingName_rejected() throws Exception {
            Map<String, Object> payload = Map.of(
                    "email", "company2@example.com",
                    "password", "s3cr3t123",
                    "role", "COMPANY");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isUnprocessableEntity());

            verify(keycloakAdminClient, never()).createUserWithRole(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("duplicate email is rejected with 409, without contacting Keycloak")
        void register_duplicateEmail_conflict() throws Exception {
            when(userService.emailExists("dup@example.com")).thenReturn(true);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(professionalPayload("dup@example.com"))))
                    .andExpect(status().isConflict());

            verify(keycloakAdminClient, never()).createUserWithRole(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Keycloak unavailable surfaces as 503")
        void register_keycloakUnavailable_serviceUnavailable() throws Exception {
            when(userService.emailExists("pro3@example.com")).thenReturn(false);
            when(keycloakAdminClient.createUserWithRole(any(), any(), any(), any(), any()))
                    .thenThrow(new KeycloakAdminException("boom"));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(professionalPayload("pro3@example.com"))))
                    .andExpect(status().isServiceUnavailable());
        }
    }
}
