package com.skillmatch.userservice.controller;

import com.skillmatch.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Reports", description = "Administrative review of user reports (ADMIN role required)")
@SecurityRequirement(name = "bearerAuth")
public class AdminReportController {

    private final UserService userService;

    @Operation(
            summary = "Close a report",
            description = "Marks a report as reviewed. Purely administrative bookkeeping — it does not suspend "
                    + "the reported user; use the separate suspend action for that."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report closed"),
            @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Report not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{reportId}/close")
    public ResponseEntity<Void> closeReport(
            @Parameter(description = "UUID of the report", required = true)
            @PathVariable("reportId") UUID reportId) {
        userService.closeReport(reportId);
        return ResponseEntity.ok().build();
    }
}
