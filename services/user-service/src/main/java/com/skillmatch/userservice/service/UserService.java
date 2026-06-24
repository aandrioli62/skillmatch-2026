package com.skillmatch.userservice.service;

import com.skillmatch.userservice.dto.request.CompanyProfileRequest;
import com.skillmatch.userservice.dto.request.ProfessionalProfileRequest;
import com.skillmatch.userservice.dto.request.UserRegistrationRequest;
import com.skillmatch.userservice.dto.response.CompanyProfileResponse;
import com.skillmatch.userservice.dto.response.ProfessionalProfileResponse;
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
     * Returns the base user record (status, role, email, timestamps).
     *
     * @throws com.skillmatch.userservice.exception.UserNotFoundException if user does not exist
     */
    UserResponse getUserProfile(UUID userId);

    /**
     * Creates or updates the professional profile for a given user.
     * The user must have role PROFESSIONAL.
     *
     * @throws com.skillmatch.userservice.exception.UserNotFoundException       if user does not exist
     * @throws com.skillmatch.userservice.exception.InvalidUserOperationException if user is not a PROFESSIONAL
     */
    ProfessionalProfileResponse updateProfessionalProfile(UUID userId, ProfessionalProfileRequest request);

    /**
     * Creates or updates the company profile for a given user.
     * The user must have role COMPANY.
     *
     * @throws com.skillmatch.userservice.exception.UserNotFoundException       if user does not exist
     * @throws com.skillmatch.userservice.exception.InvalidUserOperationException if user is not a COMPANY
     */
    CompanyProfileResponse updateCompanyProfile(UUID userId, CompanyProfileRequest request);

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
