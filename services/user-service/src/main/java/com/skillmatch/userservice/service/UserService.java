package com.skillmatch.userservice.service;

import com.skillmatch.userservice.dto.request.CompanyProfileRequest;
import com.skillmatch.userservice.dto.request.PortfolioItemRequest;
import com.skillmatch.userservice.dto.request.ProfessionalProfileRequest;
import com.skillmatch.userservice.dto.request.ProfessionalSkillRequest;
import com.skillmatch.userservice.dto.request.UserRegistrationRequest;
import com.skillmatch.userservice.dto.response.CompanyProfileResponse;
import com.skillmatch.userservice.dto.response.PortfolioItemResponse;
import com.skillmatch.userservice.dto.response.ProfessionalProfileResponse;
import com.skillmatch.userservice.dto.response.ProfessionalSkillResponse;
import com.skillmatch.userservice.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface UserService {

    /**
     * Registers a new user (PROFESSIONAL or COMPANY). Publishes user.registered event.
     * If role is PROFESSIONAL, an empty profile record is automatically created.
     *
     * @throws com.skillmatch.userservice.exception.DuplicateEmailException if email already exists
     */
    UserResponse registerUser(UserRegistrationRequest request);

    /**
     * Checks whether a user with the given email already exists, so callers that
     * provision an identity in an external system (e.g. Keycloak) before registering
     * can fail fast without creating an orphaned external account.
     */
    boolean emailExists(String email);

    /**
     * Returns the base user record (status, role, email, timestamps).
     *
     * @throws com.skillmatch.userservice.exception.UserNotFoundException if user does not exist
     */
    UserResponse getUserProfile(UUID userId);

    /**
     * Resolves the internal user record for the authenticated caller, based on the
     * Keycloak subject (JWT {@code sub} claim). Lets other services translate a JWT
     * into the platform-wide {@code users.id} used as companyId/professionalId elsewhere.
     *
     * @throws com.skillmatch.userservice.exception.UserNotFoundException if no user is linked to this Keycloak id
     */
    UserResponse getUserProfileByKeycloakId(String keycloakId);

    /**
     * Creates or updates the professional profile for a given user.
     * The user must have role PROFESSIONAL.
     *
     * @throws com.skillmatch.userservice.exception.UserNotFoundException       if user does not exist
     * @throws com.skillmatch.userservice.exception.InvalidUserOperationException if user is not a PROFESSIONAL
     */
    ProfessionalProfileResponse updateProfessionalProfile(UUID userId, ProfessionalProfileRequest request);

    /**
     * Replaces the full set of skills (and optional certification links) for a professional.
     * Skills not present in the request are removed from the professional's profile (the
     * shared skill catalog entry itself is never deleted). Unknown skill names are added to
     * the shared catalog on the fly.
     *
     * @throws com.skillmatch.userservice.exception.UserNotFoundException       if user does not exist
     * @throws com.skillmatch.userservice.exception.InvalidUserOperationException if user is not a PROFESSIONAL
     */
    List<ProfessionalSkillResponse> updateProfessionalSkills(UUID userId, List<ProfessionalSkillRequest> skills);

    /**
     * Replaces the full list of portfolio items (title, description, link) for a professional.
     *
     * @throws com.skillmatch.userservice.exception.UserNotFoundException       if user does not exist
     * @throws com.skillmatch.userservice.exception.InvalidUserOperationException if user is not a PROFESSIONAL
     */
    List<PortfolioItemResponse> updatePortfolioItems(UUID userId, List<PortfolioItemRequest> items);

    /**
     * Admin: returns the professional profile for a given user, regardless of validation
     * status — used to display the applicant's name while reviewing a pending registration.
     *
     * @throws com.skillmatch.userservice.exception.UserNotFoundException       if user does not exist
     * @throws com.skillmatch.userservice.exception.InvalidUserOperationException if user is not a PROFESSIONAL
     */
    ProfessionalProfileResponse getProfessionalProfile(UUID userId);

    /**
     * Creates or updates the company profile for a given user.
     * The user must have role COMPANY.
     *
     * @throws com.skillmatch.userservice.exception.UserNotFoundException       if user does not exist
     * @throws com.skillmatch.userservice.exception.InvalidUserOperationException if user is not a COMPANY
     */
    CompanyProfileResponse updateCompanyProfile(UUID userId, CompanyProfileRequest request);

    /**
     * Returns the company profile for a given user (self-service, used to pre-fill the edit form).
     *
     * @throws com.skillmatch.userservice.exception.UserNotFoundException       if user does not exist
     * @throws com.skillmatch.userservice.exception.InvalidUserOperationException if user is not a COMPANY
     */
    CompanyProfileResponse getCompanyProfile(UUID userId);

    /**
     * Returns all validated professionals who possess the given skill (case-insensitive match).
     */
    List<ProfessionalProfileResponse> searchProfessionalsBySkill(String skillName);

    /**
     * Admin: transitions a PROFESSIONAL from PENDING or SUSPENDED to VALIDATED.
     * Publishes user.validated event.
     *
     * @throws com.skillmatch.userservice.exception.UserNotFoundException       if user does not exist
     * @throws com.skillmatch.userservice.exception.InvalidUserOperationException if user is not a PROFESSIONAL or is already VALIDATED
     */
    UserResponse validateProfessional(UUID userId);

    /**
     * Admin: suspends a user regardless of role (PENDING → SUSPENDED, VALIDATED → SUSPENDED).
     *
     * @throws com.skillmatch.userservice.exception.UserNotFoundException       if user does not exist
     * @throws com.skillmatch.userservice.exception.InvalidUserOperationException if user is already SUSPENDED
     */
    UserResponse suspendUser(UUID userId);

    /**
     * Recalculates and persists the reputation level for a professional based on
     * aggregated feedback stats received via feedback.submitted event.
     * Thresholds (from business rules):
     *   - TOP_PERFORMER : avgRating >= 4.5 AND totalReviews >= 10
     *   - AFFIDABILE    : avgRating >= 3.5 AND totalReviews >= 3
     *   - JUNIOR        : everything else
     *
     * @throws com.skillmatch.userservice.exception.UserNotFoundException if no professional profile found for userId
     */
    void updateReputation(UUID userId, BigDecimal avgRating, Integer totalReviews);

    /**
     * Admin: returns a paginated list of all users ordered by creation date descending.
     */
    Page<UserResponse> listUsers(Pageable pageable);
}
