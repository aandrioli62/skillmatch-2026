package com.skillmatch.userservice.controller;

import com.skillmatch.userservice.dto.response.ProfessionalProfileResponse;
import com.skillmatch.userservice.dto.response.ReportResponse;
import com.skillmatch.userservice.dto.response.UserResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Users", description = "Administrative operations on user accounts (ADMIN role required)")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final UserService userService;

    // -------------------------------------------------------------------------
    // Listing
    // -------------------------------------------------------------------------

    @Operation(
            summary = "List all users (paginated)",
            description = "Returns a paginated list of all registered users sorted by creation date descending. "
                    + "Useful for monitoring and selecting accounts to validate or suspend (UC-A1, UC-A3)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated user list returned"),
            @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<Page<UserResponse>> listUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(pageable));
    }

    @Operation(
            summary = "Get a professional's profile",
            description = "Returns the professional profile (name, bio, reputation) for a given user, regardless "
                    + "of validation status — used to identify an applicant while reviewing a pending registration."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned",
                    content = @Content(schema = @Schema(implementation = ProfessionalProfileResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "User is not a PROFESSIONAL",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{userId}/professional-profile")
    public ResponseEntity<ProfessionalProfileResponse> getProfessionalProfile(
            @Parameter(description = "UUID of the professional", required = true)
            @PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(userService.getProfessionalProfile(userId));
    }

    @Operation(
            summary = "List reports filed against a user",
            description = "Returns all reports (open and closed) filed against a given user, most recent "
                    + "first — reviewed alongside the profile before deciding to validate or suspend."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reports returned"),
            @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{userId}/reports")
    public ResponseEntity<List<ReportResponse>> listReportsForUser(
            @Parameter(description = "UUID of the reported user", required = true)
            @PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(userService.listReportsForUser(userId));
    }

    // -------------------------------------------------------------------------
    // Validation (UC-A1)
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Validate a user's registration",
            description = "Transitions a user (PROFESSIONAL or COMPANY) from PENDING (or SUSPENDED) to VALIDATED "
                    + "status — required before a professional can apply to projects, or a company can publish "
                    + "one. Publishes a `user.validated` event. (UC-A1)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User validated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "User is already VALIDATED",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{userId}/validate")
    public ResponseEntity<UserResponse> validateUser(
            @Parameter(description = "UUID of the user to validate", required = true)
            @PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(userService.validateUser(userId));
    }

    // -------------------------------------------------------------------------
    // Suspension (UC-A3)
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Suspend a user account",
            description = "Transitions any user (PROFESSIONAL or COMPANY) from PENDING or VALIDATED to SUSPENDED. "
                    + "A suspended user loses access to platform features. (UC-A3)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User suspended successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "User is already SUSPENDED",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{userId}/suspend")
    public ResponseEntity<UserResponse> suspendUser(
            @Parameter(description = "UUID of the user to suspend", required = true)
            @PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(userService.suspendUser(userId));
    }
}
