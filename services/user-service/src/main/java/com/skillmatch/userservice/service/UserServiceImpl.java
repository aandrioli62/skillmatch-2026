package com.skillmatch.userservice.service;

import com.skillmatch.userservice.dto.request.CompanyProfileRequest;
import com.skillmatch.userservice.dto.request.ProfessionalProfileRequest;
import com.skillmatch.userservice.dto.request.UserRegistrationRequest;
import com.skillmatch.userservice.dto.response.CompanyProfileResponse;
import com.skillmatch.userservice.dto.response.ProfessionalProfileResponse;
import com.skillmatch.userservice.dto.response.UserResponse;
import com.skillmatch.userservice.event.UserRegisteredEvent;
import com.skillmatch.userservice.event.UserValidatedEvent;
import com.skillmatch.userservice.exception.DuplicateEmailException;
import com.skillmatch.userservice.exception.InvalidUserOperationException;
import com.skillmatch.userservice.exception.UserNotFoundException;
import com.skillmatch.userservice.mapper.CompanyProfileMapper;
import com.skillmatch.userservice.mapper.ProfessionalProfileMapper;
import com.skillmatch.userservice.mapper.UserMapper;
import com.skillmatch.userservice.model.CompanyProfile;
import com.skillmatch.userservice.model.ProfessionalProfile;
import com.skillmatch.userservice.model.User;
import com.skillmatch.userservice.model.enums.ReputationLevel;
import com.skillmatch.userservice.model.enums.UserRole;
import com.skillmatch.userservice.model.enums.UserStatus;
import com.skillmatch.userservice.repository.CompanyProfileRepository;
import com.skillmatch.userservice.repository.ProfessionalProfileRepository;
import com.skillmatch.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    // -------------------------------------------------------------------------
    // Reputation thresholds (CLAUDE.md business rules)
    // -------------------------------------------------------------------------
    private static final BigDecimal TOP_PERFORMER_MIN_RATING  = new BigDecimal("4.5");
    private static final BigDecimal AFFIDABILE_MIN_RATING     = new BigDecimal("3.5");
    private static final int        TOP_PERFORMER_MIN_REVIEWS = 10;
    private static final int        AFFIDABILE_MIN_REVIEWS    = 3;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    private final UserRepository                userRepository;
    private final ProfessionalProfileRepository professionalProfileRepository;
    private final CompanyProfileRepository      companyProfileRepository;
    private final UserEventPublisher            eventPublisher;
    private final UserMapper                    userMapper;
    private final ProfessionalProfileMapper     professionalProfileMapper;
    private final CompanyProfileMapper          companyProfileMapper;

    // =========================================================================
    // Registration
    // =========================================================================

    @Override
    public UserResponse registerUser(UserRegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user = userRepository.save(user);

        // Eagerly create an empty professional profile so the record exists
        // for subsequent updateProfessionalProfile and skill association calls.
        if (UserRole.PROFESSIONAL == user.getRole()) {
            ProfessionalProfile profile = new ProfessionalProfile();
            profile.setUser(user);
            professionalProfileRepository.save(profile);
        }

        eventPublisher.publishUserRegistered(
                UserRegisteredEvent.builder()
                        .data(UserRegisteredEvent.Data.builder()
                                .userId(user.getId())
                                .keycloakId(user.getKeycloakId())
                                .email(user.getEmail())
                                .role(user.getRole().name())
                                .build())
                        .build());

        log.info("User registered: userId={}, email={}, role={}", user.getId(), user.getEmail(), user.getRole());
        return userMapper.toResponse(user);
    }

    // =========================================================================
    // Profile retrieval
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(UUID userId) {
        return userMapper.toResponse(findUserById(userId));
    }

    // =========================================================================
    // Profile updates
    // =========================================================================

    @Override
    public ProfessionalProfileResponse updateProfessionalProfile(UUID userId, ProfessionalProfileRequest request) {
        User user = findUserById(userId);

        if (user.getRole() != UserRole.PROFESSIONAL) {
            throw new InvalidUserOperationException(
                    "User with id=" + userId + " is not a PROFESSIONAL and cannot have a professional profile.");
        }

        ProfessionalProfile profile = professionalProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    ProfessionalProfile p = new ProfessionalProfile();
                    p.setUser(user);
                    return p;
                });

        professionalProfileMapper.updateEntity(request, profile);
        profile = professionalProfileRepository.save(profile);

        log.info("Professional profile updated: userId={}", userId);
        return professionalProfileMapper.toResponse(profile);
    }

    @Override
    public CompanyProfileResponse updateCompanyProfile(UUID userId, CompanyProfileRequest request) {
        User user = findUserById(userId);

        if (user.getRole() != UserRole.COMPANY) {
            throw new InvalidUserOperationException(
                    "User with id=" + userId + " is not a COMPANY and cannot have a company profile.");
        }

        CompanyProfile profile = companyProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    CompanyProfile p = new CompanyProfile();
                    p.setUser(user);
                    return p;
                });

        companyProfileMapper.updateEntity(request, profile);
        profile = companyProfileRepository.save(profile);

        log.info("Company profile updated: userId={}", userId);
        return companyProfileMapper.toResponse(profile);
    }

    // =========================================================================
    // Search
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ProfessionalProfileResponse> searchProfessionalsBySkill(String skillName) {
        return professionalProfileRepository.findValidatedBySkillName(skillName)
                .stream()
                .map(professionalProfileMapper::toResponse)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Admin operations
    // =========================================================================

    @Override
    public UserResponse validateProfessional(UUID userId) {
        User user = findUserById(userId);

        if (user.getRole() != UserRole.PROFESSIONAL) {
            throw new InvalidUserOperationException(
                    "Only PROFESSIONAL accounts can be validated. User id=" + userId + " has role=" + user.getRole());
        }
        if (user.getStatus() == UserStatus.VALIDATED) {
            throw new InvalidUserOperationException(
                    "Professional with id=" + userId + " is already in VALIDATED status.");
        }

        user.setStatus(UserStatus.VALIDATED);
        user = userRepository.save(user);

        eventPublisher.publishUserValidated(
                UserValidatedEvent.builder()
                        .data(UserValidatedEvent.Data.builder()
                                .userId(user.getId())
                                .email(user.getEmail())
                                .build())
                        .build());

        log.info("Professional validated by admin: userId={}", userId);
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse suspendUser(UUID userId) {
        User user = findUserById(userId);

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new InvalidUserOperationException(
                    "User with id=" + userId + " is already SUSPENDED.");
        }

        user.setStatus(UserStatus.SUSPENDED);
        user = userRepository.save(user);

        log.info("User suspended by admin: userId={}", userId);
        return userMapper.toResponse(user);
    }

    // =========================================================================
    // Reputation update (triggered by feedback.submitted event)
    // =========================================================================

    @Override
    public void updateReputation(UUID userId, BigDecimal avgRating, Integer totalReviews) {
        ProfessionalProfile profile = professionalProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(
                        "No professional profile found for userId=" + userId));

        profile.setAvgRating(avgRating);
        profile.setTotalReviews(totalReviews);
        profile.setReputationLevel(calculateReputationLevel(avgRating, totalReviews));
        professionalProfileRepository.save(profile);

        log.info("Reputation updated: userId={}, avgRating={}, totalReviews={}, level={}",
                userId, avgRating, totalReviews, profile.getReputationLevel());
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Determines reputation level according to the business rules defined in CLAUDE.md:
     * <ul>
     *   <li>TOP_PERFORMER : avgRating &ge; 4.5 AND totalReviews &ge; 10</li>
     *   <li>AFFIDABILE    : avgRating &ge; 3.5 AND totalReviews &ge; 3</li>
     *   <li>JUNIOR        : everything else (avgRating &lt; 3.5 OR totalReviews &lt; 3)</li>
     * </ul>
     */
    private ReputationLevel calculateReputationLevel(BigDecimal avgRating, int totalReviews) {
        if (avgRating.compareTo(TOP_PERFORMER_MIN_RATING) >= 0 && totalReviews >= TOP_PERFORMER_MIN_REVIEWS) {
            return ReputationLevel.TOP_PERFORMER;
        }
        if (avgRating.compareTo(AFFIDABILE_MIN_RATING) >= 0 && totalReviews >= AFFIDABILE_MIN_REVIEWS) {
            return ReputationLevel.AFFIDABILE;
        }
        return ReputationLevel.JUNIOR;
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
