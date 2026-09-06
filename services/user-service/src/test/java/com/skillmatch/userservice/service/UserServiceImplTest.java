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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl — Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfessionalProfileRepository professionalProfileRepository;
    @Mock
    private CompanyProfileRepository companyProfileRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private UserSkillRepository userSkillRepository;
    @Mock
    private PortfolioItemRepository portfolioItemRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private EventPublisherService eventPublisher;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ProfessionalProfileMapper professionalProfileMapper;
    @Mock
    private CompanyProfileMapper companyProfileMapper;
    @Mock
    private SkillMapper skillMapper;
    @Mock
    private PortfolioItemMapper portfolioItemMapper;

    @InjectMocks
    private UserServiceImpl userService;

    // -------------------------------------------------------------------------
    // Test fixtures
    // -------------------------------------------------------------------------

    private UUID userId;
    private User professionalUser;
    private User companyUser;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        professionalUser = new User();
        professionalUser.setId(userId);
        professionalUser.setEmail("pro@example.com");
        professionalUser.setKeycloakId("kc-pro-001");
        professionalUser.setRole(UserRole.PROFESSIONAL);
        professionalUser.setStatus(UserStatus.PENDING);

        companyUser = new User();
        companyUser.setId(userId);
        companyUser.setEmail("company@example.com");
        companyUser.setKeycloakId("kc-co-001");
        companyUser.setRole(UserRole.COMPANY);
        companyUser.setStatus(UserStatus.PENDING);

        userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setEmail("pro@example.com");
        userResponse.setRole(UserRole.PROFESSIONAL);
        userResponse.setStatus(UserStatus.PENDING);
    }

    // =========================================================================
    // registerUser
    // =========================================================================

    @Nested
    @DisplayName("registerUser()")
    class RegisterUser {

        @Test
        @DisplayName("PROFESSIONAL registration: saves user, creates empty profile, publishes event")
        void registerProfessional_success() {
            UserRegistrationRequest request = buildRegistrationRequest("pro@example.com", "kc-pro-001", UserRole.PROFESSIONAL);

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(professionalUser);
            when(userRepository.save(professionalUser)).thenReturn(professionalUser);
            when(userMapper.toResponse(professionalUser)).thenReturn(userResponse);

            UserResponse result = userService.registerUser(request);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("pro@example.com");
            assertThat(result.getRole()).isEqualTo(UserRole.PROFESSIONAL);

            // Empty professional profile must be saved
            ArgumentCaptor<ProfessionalProfile> profileCaptor = ArgumentCaptor.forClass(ProfessionalProfile.class);
            verify(professionalProfileRepository).save(profileCaptor.capture());
            assertThat(profileCaptor.getValue().getUser()).isEqualTo(professionalUser);

            // Event must be published
            verify(eventPublisher).publishUserRegistered(any(UserRegisteredEvent.class));
            verifyNoInteractions(companyProfileRepository);
        }

        @Test
        @DisplayName("COMPANY registration: saves user, does NOT create professional profile, publishes event")
        void registerCompany_success() {
            UserRegistrationRequest request = buildRegistrationRequest("company@example.com", "kc-co-001", UserRole.COMPANY);
            UserResponse companyResponse = new UserResponse();
            companyResponse.setId(userId);
            companyResponse.setRole(UserRole.COMPANY);

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(companyUser);
            when(userRepository.save(companyUser)).thenReturn(companyUser);
            when(userMapper.toResponse(companyUser)).thenReturn(companyResponse);

            UserResponse result = userService.registerUser(request);

            assertThat(result.getRole()).isEqualTo(UserRole.COMPANY);
            verifyNoInteractions(professionalProfileRepository);
            verify(eventPublisher).publishUserRegistered(any(UserRegisteredEvent.class));
        }

        @Test
        @DisplayName("duplicate email: throws DuplicateEmailException without saving")
        void registerUser_duplicateEmail_throws() {
            UserRegistrationRequest request = buildRegistrationRequest("pro@example.com", "kc-001", UserRole.PROFESSIONAL);
            when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> userService.registerUser(request))
                    .isInstanceOf(DuplicateEmailException.class)
                    .hasMessageContaining("pro@example.com");

            verify(userRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("published event contains correct userId, email and role")
        void registerProfessional_eventPayload() {
            UserRegistrationRequest request = buildRegistrationRequest("pro@example.com", "kc-pro-001", UserRole.PROFESSIONAL);
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(professionalUser);
            when(userRepository.save(professionalUser)).thenReturn(professionalUser);
            when(userMapper.toResponse(professionalUser)).thenReturn(userResponse);

            userService.registerUser(request);

            ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
            verify(eventPublisher).publishUserRegistered(eventCaptor.capture());
            UserRegisteredEvent event = eventCaptor.getValue();
            assertThat(event.getData().getUserId()).isEqualTo(userId);
            assertThat(event.getData().getEmail()).isEqualTo("pro@example.com");
            assertThat(event.getData().getRole()).isEqualTo("PROFESSIONAL");
        }
    }

    // =========================================================================
    // emailExists
    // =========================================================================

    @Nested
    @DisplayName("emailExists()")
    class EmailExists {

        @Test
        @DisplayName("delegates to the repository")
        void emailExists_delegatesToRepository() {
            when(userRepository.existsByEmail("pro@example.com")).thenReturn(true);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

            assertThat(userService.emailExists("pro@example.com")).isTrue();
            assertThat(userService.emailExists("new@example.com")).isFalse();
        }
    }

    // =========================================================================
    // getUserProfile
    // =========================================================================

    @Nested
    @DisplayName("getUserProfile()")
    class GetUserProfile {

        @Test
        @DisplayName("existing user: returns mapped response")
        void getUserProfile_found() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(professionalUser));
            when(userMapper.toResponse(professionalUser)).thenReturn(userResponse);

            UserResponse result = userService.getUserProfile(userId);

            assertThat(result).isSameAs(userResponse);
        }

        @Test
        @DisplayName("unknown user: throws UserNotFoundException")
        void getUserProfile_notFound() {
            UUID unknown = UUID.randomUUID();
            when(userRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserProfile(unknown))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining(unknown.toString());
        }
    }

    // =========================================================================
    // updateProfessionalProfile
    // =========================================================================

    @Nested
    @DisplayName("updateProfessionalProfile()")
    class UpdateProfessionalProfile {

        @Test
        @DisplayName("creates new profile when none exists")
        void updateProfessionalProfile_createsNew() {
            ProfessionalProfileRequest request = new ProfessionalProfileRequest();
            request.setFirstName("Mario");
            request.setLastName("Rossi");

            ProfessionalProfile saved = new ProfessionalProfile();
            saved.setUser(professionalUser);
            ProfessionalProfileResponse response = new ProfessionalProfileResponse();
            response.setUserId(userId);
            response.setFirstName("Mario");

            when(userRepository.findById(userId)).thenReturn(Optional.of(professionalUser));
            when(professionalProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(professionalProfileRepository.save(any())).thenReturn(saved);
            when(professionalProfileMapper.toResponse(saved)).thenReturn(response);

            ProfessionalProfileResponse result = userService.updateProfessionalProfile(userId, request);

            assertThat(result.getFirstName()).isEqualTo("Mario");
            verify(professionalProfileMapper).updateEntity(eq(request), any(ProfessionalProfile.class));
        }

        @Test
        @DisplayName("updates existing profile")
        void updateProfessionalProfile_updatesExisting() {
            ProfessionalProfileRequest request = new ProfessionalProfileRequest();
            request.setFirstName("Luigi");
            request.setLastName("Bianchi");

            ProfessionalProfile existing = new ProfessionalProfile();
            existing.setUser(professionalUser);
            ProfessionalProfileResponse response = new ProfessionalProfileResponse();
            response.setFirstName("Luigi");

            when(userRepository.findById(userId)).thenReturn(Optional.of(professionalUser));
            when(professionalProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
            when(professionalProfileRepository.save(existing)).thenReturn(existing);
            when(professionalProfileMapper.toResponse(existing)).thenReturn(response);

            ProfessionalProfileResponse result = userService.updateProfessionalProfile(userId, request);

            assertThat(result.getFirstName()).isEqualTo("Luigi");
            verify(professionalProfileMapper).updateEntity(request, existing);
        }

        @Test
        @DisplayName("non-PROFESSIONAL user: throws InvalidUserOperationException")
        void updateProfessionalProfile_wrongRole_throws() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(companyUser));

            ProfessionalProfileRequest request = new ProfessionalProfileRequest();
            request.setFirstName("Test");
            request.setLastName("Test");

            assertThatThrownBy(() -> userService.updateProfessionalProfile(userId, request))
                    .isInstanceOf(InvalidUserOperationException.class)
                    .hasMessageContaining("not a PROFESSIONAL");

            verifyNoInteractions(professionalProfileRepository);
        }

        @Test
        @DisplayName("user not found: throws UserNotFoundException")
        void updateProfessionalProfile_userNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            ProfessionalProfileRequest request = new ProfessionalProfileRequest();
            request.setFirstName("X");
            request.setLastName("Y");

            assertThatThrownBy(() -> userService.updateProfessionalProfile(userId, request))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // =========================================================================
    // getProfessionalProfile
    // =========================================================================

    @Nested
    @DisplayName("getProfessionalProfile()")
    class GetProfessionalProfile {

        @Test
        @DisplayName("existing profile: returns mapped response")
        void getProfessionalProfile_existing_returnsMapped() {
            ProfessionalProfile existing = new ProfessionalProfile();
            existing.setUser(professionalUser);
            ProfessionalProfileResponse response = new ProfessionalProfileResponse();
            response.setFirstName("Mario");

            when(userRepository.findById(userId)).thenReturn(Optional.of(professionalUser));
            when(professionalProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
            when(professionalProfileMapper.toResponse(existing)).thenReturn(response);

            ProfessionalProfileResponse result = userService.getProfessionalProfile(userId);

            assertThat(result.getFirstName()).isEqualTo("Mario");
        }

        @Test
        @DisplayName("no profile yet: returns empty profile without throwing")
        void getProfessionalProfile_noProfileYet_returnsEmpty() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(professionalUser));
            when(professionalProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(professionalProfileMapper.toResponse(any())).thenReturn(new ProfessionalProfileResponse());

            ProfessionalProfileResponse result = userService.getProfessionalProfile(userId);

            assertThat(result).isNotNull();
            verify(professionalProfileRepository, never()).save(any());
        }

        @Test
        @DisplayName("non-PROFESSIONAL user: throws InvalidUserOperationException")
        void getProfessionalProfile_wrongRole_throws() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(companyUser));

            assertThatThrownBy(() -> userService.getProfessionalProfile(userId))
                    .isInstanceOf(InvalidUserOperationException.class);
        }

        @Test
        @DisplayName("user not found: throws UserNotFoundException")
        void getProfessionalProfile_userNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getProfessionalProfile(userId))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // =========================================================================
    // updateProfessionalSkills
    // =========================================================================

    @Nested
    @DisplayName("updateProfessionalSkills()")
    class UpdateProfessionalSkills {

        @Test
        @DisplayName("existing catalog skill: reuses it, replaces the previous skill set")
        void updateSkills_existingSkill_reusesCatalogEntry() {
            Skill javaSkill = new Skill();
            javaSkill.setId(UUID.randomUUID());
            javaSkill.setName("Java");

            ProfessionalSkillRequest request = new ProfessionalSkillRequest();
            request.setSkillName("Java");
            request.setCertificationUrl("https://cert.example.com/java");

            ProfessionalSkillResponse response = new ProfessionalSkillResponse();
            response.setSkillName("Java");

            UserSkill previous = new UserSkill();
            when(userRepository.findById(userId)).thenReturn(Optional.of(professionalUser));
            when(userSkillRepository.findByIdUserId(userId)).thenReturn(List.of(previous));
            when(skillRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(javaSkill));
            when(userSkillRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(skillMapper.toProfessionalSkillResponse(any())).thenReturn(response);

            List<ProfessionalSkillResponse> result = userService.updateProfessionalSkills(userId, List.of(request));

            assertThat(result).hasSize(1);
            verify(userSkillRepository).deleteAll(List.of(previous));
            verify(skillRepository, never()).save(any());
        }

        @Test
        @DisplayName("unknown skill name: creates it in the shared catalog")
        void updateSkills_newSkill_createsCatalogEntry() {
            ProfessionalSkillRequest request = new ProfessionalSkillRequest();
            request.setSkillName("Rust");

            Skill created = new Skill();
            created.setId(UUID.randomUUID());
            created.setName("Rust");

            when(userRepository.findById(userId)).thenReturn(Optional.of(professionalUser));
            when(userSkillRepository.findByIdUserId(userId)).thenReturn(List.of());
            when(skillRepository.findByNameIgnoreCase("Rust")).thenReturn(Optional.empty());
            when(skillRepository.save(any())).thenReturn(created);
            when(userSkillRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(skillMapper.toProfessionalSkillResponse(any())).thenReturn(new ProfessionalSkillResponse());

            userService.updateProfessionalSkills(userId, List.of(request));

            verify(skillRepository).save(argThat(skill -> "Rust".equals(skill.getName())));
        }

        @Test
        @DisplayName("non-PROFESSIONAL user: throws InvalidUserOperationException")
        void updateSkills_wrongRole_throws() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(companyUser));

            assertThatThrownBy(() -> userService.updateProfessionalSkills(userId, List.of()))
                    .isInstanceOf(InvalidUserOperationException.class);
        }
    }

    // =========================================================================
    // updatePortfolioItems
    // =========================================================================

    @Nested
    @DisplayName("updatePortfolioItems()")
    class UpdatePortfolioItems {

        @Test
        @DisplayName("replaces the previous portfolio with the new list")
        void updatePortfolioItems_success() {
            PortfolioItemRequest request = new PortfolioItemRequest();
            request.setTitle("Sito e-commerce");
            request.setUrl("https://example.com");

            PortfolioItem previous = new PortfolioItem();
            PortfolioItemResponse response = new PortfolioItemResponse();
            response.setTitle("Sito e-commerce");

            when(userRepository.findById(userId)).thenReturn(Optional.of(professionalUser));
            when(portfolioItemRepository.findByUserId(userId)).thenReturn(List.of(previous));
            when(portfolioItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(portfolioItemMapper.toResponse(any())).thenReturn(response);

            List<PortfolioItemResponse> result = userService.updatePortfolioItems(userId, List.of(request));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Sito e-commerce");
            verify(portfolioItemRepository).deleteAll(List.of(previous));
        }

        @Test
        @DisplayName("non-PROFESSIONAL user: throws InvalidUserOperationException")
        void updatePortfolioItems_wrongRole_throws() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(companyUser));

            assertThatThrownBy(() -> userService.updatePortfolioItems(userId, List.of()))
                    .isInstanceOf(InvalidUserOperationException.class);
        }
    }

    // =========================================================================
    // updateCompanyProfile
    // =========================================================================

    @Nested
    @DisplayName("updateCompanyProfile()")
    class UpdateCompanyProfile {

        @Test
        @DisplayName("COMPANY user: creates or updates profile and returns response")
        void updateCompanyProfile_success() {
            CompanyProfileRequest request = new CompanyProfileRequest();
            request.setCompanyName("Acme Srl");

            CompanyProfile saved = new CompanyProfile();
            saved.setUser(companyUser);
            CompanyProfileResponse response = new CompanyProfileResponse();
            response.setCompanyName("Acme Srl");

            when(userRepository.findById(userId)).thenReturn(Optional.of(companyUser));
            when(companyProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(companyProfileRepository.save(any())).thenReturn(saved);
            when(companyProfileMapper.toResponse(saved)).thenReturn(response);

            CompanyProfileResponse result = userService.updateCompanyProfile(userId, request);

            assertThat(result.getCompanyName()).isEqualTo("Acme Srl");
        }

        @Test
        @DisplayName("non-COMPANY user: throws InvalidUserOperationException")
        void updateCompanyProfile_wrongRole_throws() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(professionalUser));

            CompanyProfileRequest request = new CompanyProfileRequest();
            request.setCompanyName("Acme");

            assertThatThrownBy(() -> userService.updateCompanyProfile(userId, request))
                    .isInstanceOf(InvalidUserOperationException.class)
                    .hasMessageContaining("not a COMPANY");
        }
    }

    // =========================================================================
    // getCompanyProfile
    // =========================================================================

    @Nested
    @DisplayName("getCompanyProfile()")
    class GetCompanyProfile {

        @Test
        @DisplayName("existing profile: returns mapped response")
        void getCompanyProfile_existing_returnsMapped() {
            CompanyProfile existing = new CompanyProfile();
            existing.setUser(companyUser);
            CompanyProfileResponse response = new CompanyProfileResponse();
            response.setCompanyName("Acme Srl");

            when(userRepository.findById(userId)).thenReturn(Optional.of(companyUser));
            when(companyProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
            when(companyProfileMapper.toResponse(existing)).thenReturn(response);

            CompanyProfileResponse result = userService.getCompanyProfile(userId);

            assertThat(result.getCompanyName()).isEqualTo("Acme Srl");
        }

        @Test
        @DisplayName("non-COMPANY user: throws InvalidUserOperationException")
        void getCompanyProfile_wrongRole_throws() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(professionalUser));

            assertThatThrownBy(() -> userService.getCompanyProfile(userId))
                    .isInstanceOf(InvalidUserOperationException.class);
        }
    }

    // =========================================================================
    // validateProfessional
    // =========================================================================

    @Nested
    @DisplayName("validateProfessional()")
    class ValidateProfessional {

        @Test
        @DisplayName("PENDING professional: transitions to VALIDATED and publishes event")
        void validateProfessional_success() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(professionalUser));
            when(userRepository.save(professionalUser)).thenReturn(professionalUser);
            when(userMapper.toResponse(professionalUser)).thenReturn(userResponse);

            userService.validateProfessional(userId);

            assertThat(professionalUser.getStatus()).isEqualTo(UserStatus.VALIDATED);
            verify(eventPublisher).publishUserValidated(any(UserValidatedEvent.class));
        }

        @Test
        @DisplayName("already VALIDATED professional: throws InvalidUserOperationException")
        void validateProfessional_alreadyValidated_throws() {
            professionalUser.setStatus(UserStatus.VALIDATED);
            when(userRepository.findById(userId)).thenReturn(Optional.of(professionalUser));

            assertThatThrownBy(() -> userService.validateProfessional(userId))
                    .isInstanceOf(InvalidUserOperationException.class)
                    .hasMessageContaining("already in VALIDATED status");

            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("COMPANY user: throws InvalidUserOperationException")
        void validateProfessional_companyRole_throws() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(companyUser));

            assertThatThrownBy(() -> userService.validateProfessional(userId))
                    .isInstanceOf(InvalidUserOperationException.class)
                    .hasMessageContaining("Only PROFESSIONAL");
        }
    }

    // =========================================================================
    // suspendUser
    // =========================================================================

    @Nested
    @DisplayName("suspendUser()")
    class SuspendUser {

        @Test
        @DisplayName("active user: transitions to SUSPENDED")
        void suspendUser_success() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(professionalUser));
            when(userRepository.save(professionalUser)).thenReturn(professionalUser);
            when(userMapper.toResponse(professionalUser)).thenReturn(userResponse);

            userService.suspendUser(userId);

            assertThat(professionalUser.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        }

        @Test
        @DisplayName("already SUSPENDED user: throws InvalidUserOperationException")
        void suspendUser_alreadySuspended_throws() {
            professionalUser.setStatus(UserStatus.SUSPENDED);
            when(userRepository.findById(userId)).thenReturn(Optional.of(professionalUser));

            assertThatThrownBy(() -> userService.suspendUser(userId))
                    .isInstanceOf(InvalidUserOperationException.class)
                    .hasMessageContaining("already SUSPENDED");
        }
    }

    // =========================================================================
    // updateReputation
    // =========================================================================

    @Nested
    @DisplayName("updateReputation() — reputation level calculation")
    class UpdateReputation {

        @Test
        @DisplayName("avgRating=4.5 and reviews=10 → TOP_PERFORMER")
        void topPerformer() {
            assertReputation(new BigDecimal("4.5"), 10, ReputationLevel.TOP_PERFORMER);
        }

        @Test
        @DisplayName("avgRating=5.0 and reviews=15 → TOP_PERFORMER")
        void topPerformer_aboveThreshold() {
            assertReputation(new BigDecimal("5.0"), 15, ReputationLevel.TOP_PERFORMER);
        }

        @Test
        @DisplayName("avgRating=4.5 and reviews=9 → AFFIDABILE (not enough reviews for TOP)")
        void topPerformer_insufficientReviews_fallsToAffidabile() {
            assertReputation(new BigDecimal("4.5"), 9, ReputationLevel.AFFIDABILE);
        }

        @Test
        @DisplayName("avgRating=3.5 and reviews=3 → AFFIDABILE")
        void affidabile() {
            assertReputation(new BigDecimal("3.5"), 3, ReputationLevel.AFFIDABILE);
        }

        @Test
        @DisplayName("avgRating=3.5 and reviews=2 → JUNIOR (not enough reviews)")
        void affidabile_insufficientReviews_fallsToJunior() {
            assertReputation(new BigDecimal("3.5"), 2, ReputationLevel.JUNIOR);
        }

        @Test
        @DisplayName("avgRating=3.4 and reviews=10 → JUNIOR (rating too low)")
        void junior_ratingTooLow() {
            assertReputation(new BigDecimal("3.4"), 10, ReputationLevel.JUNIOR);
        }

        @Test
        @DisplayName("avgRating=0 and reviews=0 → JUNIOR")
        void junior_noReviews() {
            assertReputation(BigDecimal.ZERO, 0, ReputationLevel.JUNIOR);
        }

        private void assertReputation(BigDecimal avgRating, int totalReviews, ReputationLevel expectedLevel) {
            ProfessionalProfile profile = new ProfessionalProfile();
            profile.setUser(professionalUser);

            when(professionalProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
            when(professionalProfileRepository.save(profile)).thenReturn(profile);

            userService.updateReputation(userId, avgRating, totalReviews);

            assertThat(profile.getReputationLevel()).isEqualTo(expectedLevel);
            assertThat(profile.getAvgRating()).isEqualByComparingTo(avgRating);
            assertThat(profile.getTotalReviews()).isEqualTo(totalReviews);
        }
    }

    // =========================================================================
    // searchProfessionalsBySkill
    // =========================================================================

    @Nested
    @DisplayName("searchProfessionalsBySkill()")
    class SearchProfessionalsBySkill {

        @Test
        @DisplayName("returns mapped responses for matching profiles")
        void searchBySkill_returnsList() {
            ProfessionalProfile p = new ProfessionalProfile();
            p.setUser(professionalUser);
            ProfessionalProfileResponse response = new ProfessionalProfileResponse();
            response.setUserId(userId);

            when(professionalProfileRepository.findValidatedBySkillName("Java")).thenReturn(List.of(p));
            when(professionalProfileMapper.toResponse(p)).thenReturn(response);

            List<ProfessionalProfileResponse> results = userService.searchProfessionalsBySkill("Java");

            assertThat(results).hasSize(1).containsExactly(response);
        }

        @Test
        @DisplayName("no match: returns empty list")
        void searchBySkill_empty() {
            when(professionalProfileRepository.findValidatedBySkillName("Cobol")).thenReturn(Collections.emptyList());

            List<ProfessionalProfileResponse> results = userService.searchProfessionalsBySkill("Cobol");

            assertThat(results).isEmpty();
        }
    }

    // =========================================================================
    // listUsers
    // =========================================================================

    @Nested
    @DisplayName("listUsers()")
    class ListUsers {

        @Test
        @DisplayName("returns paginated mapped users")
        void listUsers_returnsMappedPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(List.of(professionalUser), pageable, 1);

            when(userRepository.findAll(pageable)).thenReturn(userPage);
            when(userMapper.toResponse(professionalUser)).thenReturn(userResponse);

            Page<UserResponse> result = userService.listUsers(pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).containsExactly(userResponse);
        }
    }

    // =========================================================================
    // createReport / listReportsForUser / closeReport
    // =========================================================================

    @Nested
    @DisplayName("createReport()")
    class CreateReport {

        @Test
        @DisplayName("valid report: saved as OPEN and mapped to response")
        void createReport_success() {
            User reportedUser = new User();
            reportedUser.setId(UUID.randomUUID());
            reportedUser.setEmail("company@example.com");

            ReportRequest request = new ReportRequest();
            request.setReportedUserId(reportedUser.getId());
            request.setReason("No-show alla scadenza.");

            when(userRepository.findByKeycloakId("kc-pro-001")).thenReturn(Optional.of(professionalUser));
            when(userRepository.findById(reportedUser.getId())).thenReturn(Optional.of(reportedUser));
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReportResponse result = userService.createReport("kc-pro-001", request);

            assertThat(result.getReporterEmail()).isEqualTo("pro@example.com");
            assertThat(result.getReportedUserId()).isEqualTo(reportedUser.getId());
            assertThat(result.getStatus()).isEqualTo(ReportStatus.OPEN);
        }

        @Test
        @DisplayName("reporting yourself: throws InvalidUserOperationException")
        void createReport_self_throws() {
            ReportRequest request = new ReportRequest();
            request.setReportedUserId(userId);
            request.setReason("x");

            when(userRepository.findByKeycloakId("kc-pro-001")).thenReturn(Optional.of(professionalUser));
            when(userRepository.findById(userId)).thenReturn(Optional.of(professionalUser));

            assertThatThrownBy(() -> userService.createReport("kc-pro-001", request))
                    .isInstanceOf(InvalidUserOperationException.class);
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("reporter not found: throws UserNotFoundException")
        void createReport_reporterNotFound_throws() {
            when(userRepository.findByKeycloakId("kc-unknown")).thenReturn(Optional.empty());

            ReportRequest request = new ReportRequest();
            request.setReportedUserId(UUID.randomUUID());
            request.setReason("x");

            assertThatThrownBy(() -> userService.createReport("kc-unknown", request))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listReportsForUser()")
    class ListReportsForUser {

        @Test
        @DisplayName("returns reports mapped, most recent first")
        void listReportsForUser_returnsMapped() {
            Report report = new Report();
            report.setId(UUID.randomUUID());
            report.setReporter(companyUser);
            report.setReportedUser(professionalUser);
            report.setReason("x");
            report.setStatus(ReportStatus.OPEN);

            when(reportRepository.findByReportedUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(report));

            List<ReportResponse> result = userService.listReportsForUser(userId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getReporterEmail()).isEqualTo("company@example.com");
        }
    }

    @Nested
    @DisplayName("closeReport()")
    class CloseReport {

        @Test
        @DisplayName("existing report: transitions to CLOSED")
        void closeReport_success() {
            Report report = new Report();
            UUID reportId = UUID.randomUUID();
            report.setId(reportId);
            report.setReporter(companyUser);
            report.setReportedUser(professionalUser);
            report.setStatus(ReportStatus.OPEN);

            when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));

            userService.closeReport(reportId);

            assertThat(report.getStatus()).isEqualTo(ReportStatus.CLOSED);
            verify(reportRepository).save(report);
        }

        @Test
        @DisplayName("report not found: throws ReportNotFoundException")
        void closeReport_notFound_throws() {
            UUID reportId = UUID.randomUUID();
            when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.closeReport(reportId))
                    .isInstanceOf(ReportNotFoundException.class);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private UserRegistrationRequest buildRegistrationRequest(String email, String keycloakId, UserRole role) {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setEmail(email);
        req.setKeycloakId(keycloakId);
        req.setRole(role);
        return req;
    }
}
