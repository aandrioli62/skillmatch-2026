package com.skillmatch.projectservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatch.projectservice.client.UserServiceClient;
import com.skillmatch.projectservice.config.TestSecurityConfig;
import com.skillmatch.projectservice.dto.request.ProjectCreateRequest;
import com.skillmatch.projectservice.dto.request.ProjectRequirementRequest;
import com.skillmatch.projectservice.dto.response.ProjectResponse;
import com.skillmatch.projectservice.exception.GlobalExceptionHandler;
import com.skillmatch.projectservice.exception.InvalidProjectOperationException;
import com.skillmatch.projectservice.exception.ProjectNotFoundException;
import com.skillmatch.projectservice.model.enums.ProjectStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("ProjectController — @WebMvcTest")
class ProjectControllerTest {

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

    private ProjectCreateRequest buildCreateRequest() {
        ProjectRequirementRequest requirement = new ProjectRequirementRequest();
        requirement.setSkillName("Figma");
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setTitle("Consulenza UI/UX");
        request.setRequirements(List.of(requirement));
        return request;
    }

    // =========================================================================
    // POST /api/v1/projects
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/projects")
    class CreateProject {

        @Test
        @DisplayName("valid request by COMPANY → 201 Created")
        void createProject_success() throws Exception {
            UUID companyId = UUID.randomUUID();
            ProjectResponse response = new ProjectResponse();
            response.setId(UUID.randomUUID());
            response.setStatus(ProjectStatus.DRAFT);

            when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
            when(projectService.createProject(eq(companyId), any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/projects")
                            .with(jwt().authorities(ROLE_COMPANY))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("DRAFT"));
        }

        @Test
        @DisplayName("PROFESSIONAL role → 403 Forbidden")
        void createProject_wrongRole_forbidden() throws Exception {
            mockMvc.perform(post("/api/v1/projects")
                            .with(jwt().authorities(ROLE_PROFESSIONAL))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("no token → 401 Unauthorized")
        void createProject_noToken_unauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("missing title → 400 Bad Request")
        void createProject_missingTitle_badRequest() throws Exception {
            ProjectCreateRequest request = buildCreateRequest();
            request.setTitle(null);

            mockMvc.perform(post("/api/v1/projects")
                            .with(jwt().authorities(ROLE_COMPANY))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("empty requirements list → 400 Bad Request")
        void createProject_emptyRequirements_badRequest() throws Exception {
            ProjectCreateRequest request = buildCreateRequest();
            request.setRequirements(List.of());

            mockMvc.perform(post("/api/v1/projects")
                            .with(jwt().authorities(ROLE_COMPANY))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // PUT /api/v1/projects/{projectId}/publish
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/v1/projects/{projectId}/publish")
    class PublishProject {

        @Test
        @DisplayName("owning company → 200 OK")
        void publishProject_success() throws Exception {
            UUID companyId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            ProjectResponse response = new ProjectResponse();
            response.setStatus(ProjectStatus.OPEN);

            when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
            when(projectService.publishProject(companyId, projectId)).thenReturn(response);

            mockMvc.perform(put("/api/v1/projects/{projectId}/publish", projectId)
                            .with(jwt().authorities(ROLE_COMPANY)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("OPEN"));
        }

        @Test
        @DisplayName("not the owner → 422 Unprocessable Entity")
        void publishProject_notOwner_unprocessable() throws Exception {
            UUID companyId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();

            when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
            when(projectService.publishProject(companyId, projectId))
                    .thenThrow(new InvalidProjectOperationException("not the owner"));

            mockMvc.perform(put("/api/v1/projects/{projectId}/publish", projectId)
                            .with(jwt().authorities(ROLE_COMPANY)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.title").value("Invalid Project Operation"));
        }
    }

    // =========================================================================
    // GET /api/v1/projects/{projectId}
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/projects/{projectId}")
    class GetProject {

        @Test
        @DisplayName("existing project → 200 OK")
        void getProject_found() throws Exception {
            UUID projectId = UUID.randomUUID();
            ProjectResponse response = new ProjectResponse();
            response.setId(projectId);

            when(projectService.getProject(projectId)).thenReturn(response);

            mockMvc.perform(get("/api/v1/projects/{projectId}", projectId).with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(projectId.toString()));
        }

        @Test
        @DisplayName("unknown project → 404 Not Found")
        void getProject_notFound() throws Exception {
            UUID projectId = UUID.randomUUID();
            when(projectService.getProject(projectId)).thenThrow(new ProjectNotFoundException(projectId));

            mockMvc.perform(get("/api/v1/projects/{projectId}", projectId).with(jwt()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Project Not Found"));
        }
    }

    // =========================================================================
    // GET /api/v1/projects/open
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/projects/open")
    class ListOpenProjects {

        @Test
        @DisplayName("returns list of open projects → 200 OK")
        void listOpenProjects_success() throws Exception {
            when(projectService.listOpenProjects()).thenReturn(List.of(new ProjectResponse()));

            mockMvc.perform(get("/api/v1/projects/open").with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    // =========================================================================
    // GET /api/v1/projects/mine
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/projects/mine")
    class ListMyProjects {

        @Test
        @DisplayName("COMPANY caller → 200 OK with own projects")
        void listMyProjects_success() throws Exception {
            UUID companyId = UUID.randomUUID();
            when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
            when(projectService.listProjectsByCompany(companyId)).thenReturn(List.of(new ProjectResponse()));

            mockMvc.perform(get("/api/v1/projects/mine").with(jwt().authorities(ROLE_COMPANY)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("PROFESSIONAL role → 403 Forbidden")
        void listMyProjects_wrongRole_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/projects/mine").with(jwt().authorities(ROLE_PROFESSIONAL)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // PUT /api/v1/projects/{projectId}/complete
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/v1/projects/{projectId}/complete")
    class CompleteProject {

        @Test
        @DisplayName("owning company → 200 OK")
        void completeProject_success() throws Exception {
            UUID companyId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            ProjectResponse response = new ProjectResponse();
            response.setStatus(ProjectStatus.COMPLETED);

            when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
            when(projectService.completeProject(companyId, projectId)).thenReturn(response);

            mockMvc.perform(put("/api/v1/projects/{projectId}/complete", projectId)
                            .with(jwt().authorities(ROLE_COMPANY)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("no accepted candidature → 422 Unprocessable Entity")
        void completeProject_invalidState_unprocessable() throws Exception {
            UUID companyId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();

            when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
            when(projectService.completeProject(companyId, projectId))
                    .thenThrow(new InvalidProjectOperationException("has no accepted candidature"));

            mockMvc.perform(put("/api/v1/projects/{projectId}/complete", projectId)
                            .with(jwt().authorities(ROLE_COMPANY)))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
