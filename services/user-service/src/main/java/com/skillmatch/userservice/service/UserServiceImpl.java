package com.skillmatch.userservice.service;

import com.skillmatch.userservice.dto.request.CompanyProfileRequest;
import com.skillmatch.userservice.dto.request.PortfolioItemRequest;
import com.skillmatch.userservice.dto.request.ProfessionalProfileRequest;
import com.skillmatch.userservice.dto.request.ProfessionalSkillRequest;
import com.skillmatch.userservice.dto.request.ReportRequest;
import com.skillmatch.userservice.dto.request.UserRegistrationRequest;
import com.skillmatch.userservice.dto.response.CompanyProfileResponse;
import com.skillmatch.userservice.dto.response.PortfolioItemResponse;
import com.skillmatch.userservice.dto.response.ProfessionalProfileResponse;
import com.skillmatch.userservice.dto.response.ProfessionalSkillResponse;
import com.skillmatch.userservice.dto.response.ReportResponse;
import com.skillmatch.userservice.dto.response.UserResponse;
import com.skillmatch.userservice.event.UserRegisteredEvent;
import com.skillmatch.userservice.event.UserValidatedEvent;
import com.skillmatch.userservice.exception.DuplicateEmailException;
import com.skillmatch.userservice.exception.InvalidUserOperationException;
import com.skillmatch.userservice.exception.ReportNotFoundException;
import com.skillmatch.userservice.exception.UserNotFoundException;
import com.skillmatch.userservice.mapper.CompanyProfileMapper;
import com.skillmatch.userservice.mapper.PortfolioItemMapper;
import com.skillmatch.userservice.mapper.ProfessionalProfileMapper;
import com.skillmatch.userservice.mapper.SkillMapper;
import com.skillmatch.userservice.mapper.UserMapper;
import com.skillmatch.userservice.model.CompanyProfile;
import com.skillmatch.userservice.model.PortfolioItem;
import com.skillmatch.userservice.model.ProfessionalProfile;
import com.skillmatch.userservice.model.Report;
import com.skillmatch.userservice.model.Skill;
import com.skillmatch.userservice.model.User;
import com.skillmatch.userservice.model.UserSkill;
import com.skillmatch.userservice.model.UserSkillId;
import com.skillmatch.userservice.model.enums.ReportStatus;
import com.skillmatch.userservice.model.enums.ReputationLevel;
import com.skillmatch.userservice.model.enums.UserRole;
import com.skillmatch.userservice.model.enums.UserStatus;
import com.skillmatch.userservice.repository.CompanyProfileRepository;
import com.skillmatch.userservice.repository.PortfolioItemRepository;
import com.skillmatch.userservice.repository.ProfessionalProfileRepository;
import com.skillmatch.userservice.repository.ReportRepository;
import com.skillmatch.userservice.repository.SkillRepository;
import com.skillmatch.userservice.repository.UserRepository;
import com.skillmatch.userservice.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final SkillRepository               skillRepository;
    private final UserSkillRepository           userSkillRepository;
    private final PortfolioItemRepository       portfolioItemRepository;
    private final ReportRepository              reportRepository;
    private final EventPublisherService         eventPublisher;
    private final UserMapper                    userMapper;
    private final ProfessionalProfileMapper     professionalProfileMapper;
    private final CompanyProfileMapper          companyProfileMapper;
    private final SkillMapper                   skillMapper;
    private final PortfolioItemMapper           portfolioItemMapper;

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

    @Override
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    // =========================================================================
    // Profile retrieval
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(UUID userId) {
        return userMapper.toResponse(findUserById(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserProfileByKeycloakId(String keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException("No user found for keycloakId: " + keycloakId));
        return userMapper.toResponse(user);
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
        return enrichWithSkillsAndPortfolio(professionalProfileMapper.toResponse(profile), userId);
    }

    @Override
    public List<ProfessionalSkillResponse> updateProfessionalSkills(UUID userId, List<ProfessionalSkillRequest> skills) {
        User user = findUserById(userId);
        requireProfessional(user, userId);

        userSkillRepository.deleteAll(userSkillRepository.findByIdUserId(userId));

        List<UserSkill> saved = skills.stream()
                .map(request -> {
                    Skill skill = skillRepository.findByNameIgnoreCase(request.getSkillName())
                            .orElseGet(() -> {
                                Skill newSkill = new Skill();
                                newSkill.setName(request.getSkillName());
                                return skillRepository.save(newSkill);
                            });

                    UserSkill userSkill = new UserSkill();
                    userSkill.setId(new UserSkillId(userId, skill.getId()));
                    userSkill.setUser(user);
                    userSkill.setSkill(skill);
                    userSkill.setCertificationUrl(request.getCertificationUrl());
                    return userSkillRepository.save(userSkill);
                })
                .toList();

        log.info("Professional skills updated: userId={}, count={}", userId, saved.size());
        return saved.stream().map(skillMapper::toProfessionalSkillResponse).toList();
    }

    @Override
    public List<PortfolioItemResponse> updatePortfolioItems(UUID userId, List<PortfolioItemRequest> items) {
        User user = findUserById(userId);
        requireProfessional(user, userId);

        portfolioItemRepository.deleteAll(portfolioItemRepository.findByUserId(userId));

        List<PortfolioItem> saved = items.stream()
                .map(request -> {
                    PortfolioItem item = new PortfolioItem();
                    item.setUser(user);
                    item.setTitle(request.getTitle());
                    item.setDescription(request.getDescription());
                    item.setUrl(request.getUrl());
                    return portfolioItemRepository.save(item);
                })
                .toList();

        log.info("Portfolio items updated: userId={}, count={}", userId, saved.size());
        return saved.stream().map(portfolioItemMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProfessionalProfileResponse getProfessionalProfile(UUID userId) {
        User user = findUserById(userId);
        requireProfessional(user, userId);

        ProfessionalProfile profile = professionalProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    ProfessionalProfile p = new ProfessionalProfile();
                    p.setUser(user);
                    return p;
                });

        return enrichWithSkillsAndPortfolio(professionalProfileMapper.toResponse(profile), userId);
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

    @Override
    @Transactional(readOnly = true)
    public CompanyProfileResponse getCompanyProfile(UUID userId) {
        User user = findUserById(userId);

        if (user.getRole() != UserRole.COMPANY) {
            throw new InvalidUserOperationException(
                    "User with id=" + userId + " is not a COMPANY.");
        }

        CompanyProfile profile = companyProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    CompanyProfile p = new CompanyProfile();
                    p.setUser(user);
                    return p;
                });

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

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(user -> {
            UserResponse response = userMapper.toResponse(user);
            long openReports = reportRepository.countByReportedUserIdAndStatus(user.getId(), ReportStatus.OPEN);
            response.setOpenReportCount((int) openReports);
            return response;
        });
    }

    // =========================================================================
    // Reports
    // =========================================================================

    @Override
    public ReportResponse createReport(String reporterKeycloakId, ReportRequest request) {
        User reporter = userRepository.findByKeycloakId(reporterKeycloakId)
                .orElseThrow(() -> new UserNotFoundException("No user found for keycloakId: " + reporterKeycloakId));
        User reportedUser = findUserById(request.getReportedUserId());

        if (reporter.getId().equals(reportedUser.getId())) {
            throw new InvalidUserOperationException("You cannot report yourself.");
        }

        Report report = new Report();
        report.setReporter(reporter);
        report.setReportedUser(reportedUser);
        report.setReason(request.getReason());
        report = reportRepository.save(report);

        log.info("Report filed: reporterId={}, reportedUserId={}", reporter.getId(), reportedUser.getId());
        return toReportResponse(report);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> listReportsForUser(UUID userId) {
        return reportRepository.findByReportedUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toReportResponse)
                .toList();
    }

    @Override
    public void closeReport(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));
        report.setStatus(ReportStatus.CLOSED);
        reportRepository.save(report);

        log.info("Report closed: reportId={}", reportId);
    }

    private ReportResponse toReportResponse(Report report) {
        ReportResponse response = new ReportResponse();
        response.setId(report.getId());
        response.setReporterId(report.getReporter().getId());
        response.setReporterEmail(report.getReporter().getEmail());
        response.setReportedUserId(report.getReportedUser().getId());
        response.setReason(report.getReason());
        response.setStatus(report.getStatus());
        response.setCreatedAt(report.getCreatedAt());
        return response;
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

    private void requireProfessional(User user, UUID userId) {
        if (user.getRole() != UserRole.PROFESSIONAL) {
            throw new InvalidUserOperationException(
                    "User with id=" + userId + " is not a PROFESSIONAL.");
        }
    }

    private ProfessionalProfileResponse enrichWithSkillsAndPortfolio(ProfessionalProfileResponse response, UUID userId) {
        response.setSkills(userSkillRepository.findByIdUserId(userId).stream()
                .map(skillMapper::toProfessionalSkillResponse)
                .toList());
        response.setPortfolioItems(portfolioItemRepository.findByUserId(userId).stream()
                .map(portfolioItemMapper::toResponse)
                .toList());
        return response;
    }
}
