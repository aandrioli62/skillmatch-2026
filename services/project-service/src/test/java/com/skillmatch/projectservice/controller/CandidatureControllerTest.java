package com.skillmatch.projectservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatch.projectservice.client.UserServiceClient;
import com.skillmatch.projectservice.config.TestSecurityConfig;
import com.skillmatch.projectservice.dto.request.CandidatureRequest;
import com.skillmatch.projectservice.dto.response.CandidatureResponse;
import com.skillmatch.projectservice.exception.GlobalExceptionHandler;
import com.skillmatch.projectservice.exception.InvalidProjectOperationException;
import com.skillmatch.projectservice.exception.UserServiceUnavailableException;
import com.skillmatch.projectservice.model.enums.CandidatureStatus;
import com.skillmatch.projectservice.service.ProjectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CandidatureController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("CandidatureController — @WebMvcTest")
class CandidatureControllerTest {

    private static final SimpleGrantedAuthority ROLE_COMPANY = new SimpleGrantedAuthority("ROLE_COMPANY");
    private static final SimpleGrantedAuthority ROLE_PROFESSIONAL = new SimpleGrantedAuthority("ROLE_PROFESSIONAL");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private UserServiceClient userServiceClient;

    // =========================================================================
    // POST /api/v1/projects/{projectId}/candidatures
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/projects/{projectId}/candidatures")
    class ApplyToProject {

        @Test
        @DisplayName("VALIDATED professional → 201 Created")
        void applyToProject_success() throws Exception {
            UUID professionalId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            CandidatureRequest request = new CandidatureRequest();
            request.setCoverLetter("Sono interessato");
            CandidatureResponse response = new CandidatureResponse();
            response.setStatus(CandidatureStatus.PENDING);

            when(userServiceClient.resolveCurrentUserId()).thenReturn(professionalId);
            when(projectService.applyToProject(eq(professionalId), eq(projectId), any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/projects/{projectId}/candidatures", projectId)
                            .with(jwt().authorities(ROLE_PROFESSIONAL))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("COMPANY role → 403 Forbidden")
        void applyToProject_wrongRole_forbidden() throws Exception {
            UUID projectId = UUID.randomUUID();
            CandidatureRequest request = new CandidatureRequest();

            mockMvc.perform(post("/api/v1/projects/{projectId}/candidatures", projectId)
                            .with(jwt().authorities(ROLE_COMPANY))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("professional not VALIDATED → 422 Unprocessable Entity")
        void applyToProject_notValidated_unprocessable() throws Exception {
            UUID professionalId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            CandidatureRequest request = new CandidatureRequest();

            when(userServiceClient.resolveCurrentUserId()).thenReturn(professionalId);
            when(projectService.applyToProject(eq(professionalId), eq(projectId), any()))
                    .thenThrow(new InvalidProjectOperationException("must be VALIDATED"));

            mockMvc.perform(post("/api/v1/projects/{projectId}/candidatures", projectId)
                            .with(jwt().authorities(ROLE_PROFESSIONAL))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("User Service unavailable → 503 Service Unavailable")
        void applyToProject_userServiceDown_serviceUnavailable() throws Exception {
            UUID projectId = UUID.randomUUID();
            CandidatureRequest request = new CandidatureRequest();

            when(userServiceClient.resolveCurrentUserId())
                    .thenThrow(new UserServiceUnavailableException("User Service is unavailable", null));

            mockMvc.perform(post("/api/v1/projects/{projectId}/candidatures", projectId)
                            .with(jwt().authorities(ROLE_PROFESSIONAL))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isServiceUnavailable());
        }
    }

    // =========================================================================
    // GET /api/v1/projects/candidatures/mine
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/projects/candidatures/mine")
    class ListMyCandidatures {

        @Test
        @DisplayName("PROFESSIONAL caller → 200 OK with own candidatures")
        void listMyCandidatures_success() throws Exception {
            UUID professionalId = UUID.randomUUID();
            when(userServiceClient.resolveCurrentUserId()).thenReturn(professionalId);
            when(projectService.listCandidaturesByProfessional(professionalId))
                    .thenReturn(List.of(new CandidatureResponse()));

            mockMvc.perform(get("/api/v1/projects/candidatures/mine").with(jwt().authorities(ROLE_PROFESSIONAL)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("COMPANY role → 403 Forbidden")
        void listMyCandidatures_wrongRole_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/projects/candidatures/mine").with(jwt().authorities(ROLE_COMPANY)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // PUT /api/v1/projects/{projectId}/candidatures/{candidatureId}/accept
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/v1/projects/{projectId}/candidatures/{candidatureId}/accept")
    class AcceptCandidature {

        @Test
        @DisplayName("owning company → 200 OK")
        void acceptCandidature_success() throws Exception {
            UUID companyId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            UUID candidatureId = UUID.randomUUID();
            CandidatureResponse response = new CandidatureResponse();
            response.setStatus(CandidatureStatus.ACCEPTED);

            when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
            when(projectService.acceptCandidature(companyId, projectId, candidatureId)).thenReturn(response);

            mockMvc.perform(put("/api/v1/projects/{projectId}/candidatures/{candidatureId}/accept", projectId, candidatureId)
                            .with(jwt().authorities(ROLE_COMPANY)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACCEPTED"));
        }

        @Test
        @DisplayName("PROFESSIONAL role → 403 Forbidden")
        void acceptCandidature_wrongRole_forbidden() throws Exception {
            UUID projectId = UUID.randomUUID();
            UUID candidatureId = UUID.randomUUID();

            mockMvc.perform(put("/api/v1/projects/{projectId}/candidatures/{candidatureId}/accept", projectId, candidatureId)
                            .with(jwt().authorities(ROLE_PROFESSIONAL)))
                    .andExpect(status().isForbidden());
        }
    }
}
