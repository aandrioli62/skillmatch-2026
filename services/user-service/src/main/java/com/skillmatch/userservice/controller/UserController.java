package com.skillmatch.userservice.controller;

import com.skillmatch.userservice.dto.request.CompanyProfileRequest;
import com.skillmatch.userservice.dto.request.ProfessionalProfileRequest;
import com.skillmatch.userservice.dto.request.UserRegistrationRequest;
import com.skillmatch.userservice.dto.response.CompanyProfileResponse;
import com.skillmatch.userservice.dto.response.ProfessionalProfileResponse;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "Users", description = "User registration and profile management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Register a new user",
            description = "Creates a user record linked to an existing Keycloak identity. "
                    + "Should be called by the frontend immediately after successful Keycloak registration."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -------------------------------------------------------------------------
    // Profile retrieval
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get user profile",
            description = "Returns the base user record (status, role, email, timestamps) for the given user ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile returned",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserProfile(
            @Parameter(description = "User UUID", required = true)
            @PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    // -------------------------------------------------------------------------
    // Professional profile
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Create or update professional profile",
            description = "Creates or updates the professional profile (name, bio, payment account) "
                    + "for the authenticated professional. The user must have role PROFESSIONAL."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Professional profile updated",
                    content = @Content(schema = @Schema(implementation = ProfessionalProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "User is not a PROFESSIONAL",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{userId}/professional-profile")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<ProfessionalProfileResponse> updateProfessionalProfile(
            @Parameter(description = "User UUID", required = true)
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody ProfessionalProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfessionalProfile(userId, request));
    }

    // -------------------------------------------------------------------------
    // Company profile
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Create or update company profile",
            description = "Creates or updates the company profile (company name, VAT, address) "
                    + "for the authenticated company. The user must have role COMPANY."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company profile updated",
                    content = @Content(schema = @Schema(implementation = CompanyProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "User is not a COMPANY",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{userId}/company-profile")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<CompanyProfileResponse> updateCompanyProfile(
            @Parameter(description = "User UUID", required = true)
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody CompanyProfileRequest request) {
        return ResponseEntity.ok(userService.updateCompanyProfile(userId, request));
    }

    // -------------------------------------------------------------------------
    // Professional search
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Search validated professionals by skill",
            description = "Returns all validated professionals who have the specified skill "
                    + "(case-insensitive match against the skill name)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching professionals returned",
                    content = @Content(schema = @Schema(implementation = ProfessionalProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Skill name is blank",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/professionals/search")
    public ResponseEntity<List<ProfessionalProfileResponse>> searchProfessionalsBySkill(
            @Parameter(description = "Skill name to search for", required = true, example = "Java")
            @RequestParam("skill") @NotBlank(message = "Skill name must not be blank") String skill) {
        return ResponseEntity.ok(userService.searchProfessionalsBySkill(skill));
    }
}
