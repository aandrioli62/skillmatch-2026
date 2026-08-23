package com.skillmatch.projectservice.controller;

import com.skillmatch.projectservice.client.UserServiceClient;
import com.skillmatch.projectservice.dto.request.ProjectCreateRequest;
import com.skillmatch.projectservice.dto.response.ProjectResponse;
import com.skillmatch.projectservice.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project publication and lifecycle management")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;
    private final UserServiceClient userServiceClient;

    // -------------------------------------------------------------------------
    // Creation & publication
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Create a new project",
            description = "Creates a project in DRAFT status for the authenticated company, together with its "
                    + "skill requirements. The project is not visible to professionals until published. (UC-C1)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Project created",
                    content = @Content(schema = @Schema(implementation = ProjectResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        UUID companyId = userServiceClient.resolveCurrentUserId();
        ProjectResponse response = projectService.createProject(companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Publish a project",
            description = "Transitions a project from DRAFT to OPEN, making it visible to professionals. "
                    + "Publishes project.published. Only the owning company may publish its own project."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project published",
                    content = @Content(schema = @Schema(implementation = ProjectResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Caller is not the owner, or project is not in DRAFT status",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{projectId}/publish")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<ProjectResponse> publishProject(
            @Parameter(description = "Project UUID", required = true)
            @PathVariable UUID projectId) {
        UUID companyId = userServiceClient.resolveCurrentUserId();
        return ResponseEntity.ok(projectService.publishProject(companyId, projectId));
    }

    // -------------------------------------------------------------------------
    // Retrieval
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get project detail",
            description = "Returns a single project with its requirements."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project returned",
                    content = @Content(schema = @Schema(implementation = ProjectResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProject(
            @Parameter(description = "Project UUID", required = true)
            @PathVariable UUID projectId) {
        return ResponseEntity.ok(projectService.getProject(projectId));
    }

    @Operation(
            summary = "List open projects",
            description = "Returns all projects currently open for candidatures. (UC-P1)"
    )
    @GetMapping("/open")
    public ResponseEntity<List<ProjectResponse>> listOpenProjects() {
        return ResponseEntity.ok(projectService.listOpenProjects());
    }

    @Operation(
            summary = "List my projects",
            description = "Returns all projects (any status) owned by the authenticated company."
    )
    @GetMapping("/mine")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<List<ProjectResponse>> listMyProjects() {
        UUID companyId = userServiceClient.resolveCurrentUserId();
        return ResponseEntity.ok(projectService.listProjectsByCompany(companyId));
    }

    // -------------------------------------------------------------------------
    // Completion
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Complete a project",
            description = "Marks an ASSIGNED/IN_PROGRESS project as COMPLETED. Publishes project.completed, "
                    + "enabling payment. Only the owning company may complete its own project. (UC-C3)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project completed",
                    content = @Content(schema = @Schema(implementation = ProjectResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Caller is not the owner, project has no accepted "
                    + "candidature, or is not ASSIGNED/IN_PROGRESS",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{projectId}/complete")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<ProjectResponse> completeProject(
            @Parameter(description = "Project UUID", required = true)
            @PathVariable UUID projectId) {
        UUID companyId = userServiceClient.resolveCurrentUserId();
        return ResponseEntity.ok(projectService.completeProject(companyId, projectId));
    }
}
