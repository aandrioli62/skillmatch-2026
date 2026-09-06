package com.skillmatch.userservice.controller;

import com.skillmatch.userservice.dto.request.ReportRequest;
import com.skillmatch.userservice.dto.response.ReportResponse;
import com.skillmatch.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Reporting the counterparty of a collaboration")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final UserService userService;

    @Operation(
            summary = "File a report against another user",
            description = "Files a report against the counterparty of a collaboration (e.g. a problematic "
                    + "contract), for an admin to review. Purely informational — does not itself suspend anyone."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Report filed",
                    content = @Content(schema = @Schema(implementation = ReportResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Reported user not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Caller tried to report themselves",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('PROFESSIONAL', 'COMPANY')")
    public ResponseEntity<ReportResponse> createReport(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReportRequest request) {
        ReportResponse response = userService.createReport(jwt.getSubject(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
