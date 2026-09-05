package com.skillmatch.userservice.dto.request;

import com.skillmatch.userservice.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Self-service sign-up: creates the Keycloak identity, the {@code users} row and
 * the minimal role-specific profile in one call. Only PROFESSIONAL and COMPANY can
 * self-register — ADMIN accounts are provisioned out of band.
 * <p>
 * {@code firstName}/{@code lastName} are required when {@code role} is PROFESSIONAL;
 * {@code companyName} is required when {@code role} is COMPANY. This is validated in
 * {@code AuthController} rather than via annotations, since the requirement depends
 * on the chosen role.
 */
@Getter
@Setter
public class SelfRegistrationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotNull(message = "Role is required")
    private UserRole role;

    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Size(max = 255, message = "Company name must not exceed 255 characters")
    private String companyName;
}
