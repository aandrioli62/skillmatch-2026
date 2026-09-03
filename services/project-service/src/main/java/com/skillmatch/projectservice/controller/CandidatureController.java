package com.skillmatch.projectservice.controller;

import com.skillmatch.projectservice.client.UserServiceClient;
import com.skillmatch.projectservice.dto.request.CandidatureRequest;
import com.skillmatch.projectservice.dto.response.CandidatureResponse;
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
@Tag(name = "Candidatures", description = "Candidature submission and selection")
@SecurityRequirement(name = "bearerAuth")
public class CandidatureController {

    private final ProjectService projectService;
    private final UserServiceClient userServiceClient;

    @Operation(
            summary = "Apply to a project",
            description = "Submits a candidature for an OPEN project. The caller must be a VALIDATED "
                    + "professional and must not have already applied. (UC-P2)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Candidature submitted",
                    content = @Content(schema = @Schema(implementation = CandidatureResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Project is not OPEN, professional is not VALIDATED, "
                    + "or a candidature already exists for this pair",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "User Service unavailable — professional status "
                    + "could not be verified",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{projectId}/candidatures")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<CandidatureResponse> applyToProject(
            @Parameter(description = "Project UUID", required = true)
            @PathVariable UUID projectId,
            @Valid @RequestBody CandidatureRequest request) {
        UUID professionalId = userServiceClient.resolveCurrentUserId();
        CandidatureResponse response = projectService.applyToProject(professionalId, projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "List my candidatures",
            description = "Returns all candidatures submitted by the authenticated professional, across all projects."
    )
    @GetMapping("/candidatures/mine")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<List<CandidatureResponse>> listMyCandidatures() {
        UUID professionalId = userServiceClient.resolveCurrentUserId();
        return ResponseEntity.ok(projectService.listCandidaturesByProfessional(professionalId));
    }

    @Operation(
            summary = "List candidatures for a project",
            description = "Returns all candidatures submitted for the project. Only the owning company may list them."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Candidatures returned"),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Caller is not the owner",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{projectId}/candidatures")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<List<CandidatureResponse>> listCandidaturesForProject(
            @Parameter(description = "Project UUID", required = true)
            @PathVariable UUID projectId) {
        UUID companyId = userServiceClient.resolveCurrentUserId();
        return ResponseEntity.ok(projectService.listCandidaturesByProject(companyId, projectId));
    }

    @Operation(
            summary = "Accept a candidature",
            description = "Accepts a candidature for the project; automatically rejects the remaining PENDING "
                    + "ones and transitions the project to ASSIGNED. Publishes candidature.accepted. (UC-C2)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Candidature accepted",
                    content = @Content(schema = @Schema(implementation = CandidatureResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project or candidature not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Caller is not the owner, candidature does not "
                    + "belong to the project, or the project already has an accepted candidature",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{projectId}/candidatures/{candidatureId}/accept")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<CandidatureResponse> acceptCandidature(
            @Parameter(description = "Project UUID", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "Candidature UUID", required = true)
            @PathVariable UUID candidatureId) {
        UUID companyId = userServiceClient.resolveCurrentUserId();
        return ResponseEntity.ok(projectService.acceptCandidature(companyId, projectId, candidatureId));
    }
}
