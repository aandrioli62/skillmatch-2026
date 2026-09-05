package com.skillmatch.userservice.controller;

import com.skillmatch.userservice.client.KeycloakAdminClient;
import com.skillmatch.userservice.dto.request.CompanyProfileRequest;
import com.skillmatch.userservice.dto.request.ProfessionalProfileRequest;
import com.skillmatch.userservice.dto.request.SelfRegistrationRequest;
import com.skillmatch.userservice.dto.request.UserRegistrationRequest;
import com.skillmatch.userservice.dto.response.UserResponse;
import com.skillmatch.userservice.exception.DuplicateEmailException;
import com.skillmatch.userservice.exception.InvalidUserOperationException;
import com.skillmatch.userservice.model.enums.UserRole;
import com.skillmatch.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Self-service account creation")
public class AuthController {

    private final UserService userService;
    private final KeycloakAdminClient keycloakAdminClient;

    @Operation(
            summary = "Self-service registration",
            description = "Creates the Keycloak identity, the user record and the minimal role-specific "
                    + "profile (name for professionals, company name for companies) in a single call. "
                    + "Only PROFESSIONAL and COMPANY may self-register."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Role is ADMIN, or a required field for the chosen role is missing",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "Keycloak is unreachable",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody SelfRegistrationRequest request) {
        if (request.getRole() == UserRole.ADMIN) {
            throw new InvalidUserOperationException("ADMIN accounts cannot be created through self-service registration.");
        }
        if (request.getRole() == UserRole.PROFESSIONAL
                && (isBlank(request.getFirstName()) || isBlank(request.getLastName()))) {
            throw new InvalidUserOperationException("First name and last name are required for PROFESSIONAL registration.");
        }
        if (request.getRole() == UserRole.COMPANY && isBlank(request.getCompanyName())) {
            throw new InvalidUserOperationException("Company name is required for COMPANY registration.");
        }
        if (userService.emailExists(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        // Keycloak's own profile just needs non-blank names to satisfy its default
        // VERIFY_PROFILE requirement; the real company name lives in CompanyProfile.
        String keycloakFirstName = request.getRole() == UserRole.PROFESSIONAL
                ? request.getFirstName() : request.getCompanyName();
        String keycloakLastName = request.getRole() == UserRole.PROFESSIONAL
                ? request.getLastName() : "Azienda";

        String keycloakId = keycloakAdminClient.createUserWithRole(
                request.getEmail(), request.getPassword(), request.getRole().name(),
                keycloakFirstName, keycloakLastName);

        UserRegistrationRequest registrationRequest = new UserRegistrationRequest();
        registrationRequest.setKeycloakId(keycloakId);
        registrationRequest.setEmail(request.getEmail());
        registrationRequest.setRole(request.getRole());
        UserResponse user = userService.registerUser(registrationRequest);

        if (request.getRole() == UserRole.PROFESSIONAL) {
            ProfessionalProfileRequest profileRequest = new ProfessionalProfileRequest();
            profileRequest.setFirstName(request.getFirstName());
            profileRequest.setLastName(request.getLastName());
            userService.updateProfessionalProfile(user.getId(), profileRequest);
        } else {
            CompanyProfileRequest profileRequest = new CompanyProfileRequest();
            profileRequest.setCompanyName(request.getCompanyName());
            userService.updateCompanyProfile(user.getId(), profileRequest);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
