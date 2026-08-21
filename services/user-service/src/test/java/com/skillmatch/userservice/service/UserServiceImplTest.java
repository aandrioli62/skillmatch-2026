package com.skillmatch.userservice.service;

import com.skillmatch.userservice.dto.request.CompanyProfileRequest;
import com.skillmatch.userservice.dto.request.ProfessionalProfileRequest;
import com.skillmatch.userservice.dto.request.UserRegistrationRequest;
import com.skillmatch.userservice.dto.response.CompanyProfileResponse;
import com.skillmatch.userservice.dto.response.ProfessionalProfileResponse;
import com.skillmatch.userservice.dto.response.UserResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfessionalProfileRepository professionalProfileRepository;
    @Mock
    private CompanyProfileRepository companyProfileRepository;
    @Mock
    private EventPublisherService eventPublisher;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ProfessionalProfileMapper professionalProfileMapper;
    @Mock
    private CompanyProfileMapper companyProfileMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;
    @Captor
    private ArgumentCaptor<ProfessionalProfile> professionalProfileCaptor;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setKeycloakId("kc-123");
        user.setEmail("professional@example.com");
        user.setRole(UserRole.PROFESSIONAL);
        user.setStatus(UserStatus.PENDING);
    }

    // =========================================================================
    // registerUser
    // =========================================================================

    @Nested
    class RegisterUser {

        @Test
        void registeringAProfessionalCreatesAnEmptyProfileAndPublishesEvent() {
            UserRegistrationRequest request = new UserRegistrationRequest();
            request.setKeycloakId("kc-123");
            request.setEmail("professional@example.com");
            request.setRole(UserRole.PROFESSIONAL);

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(user);
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(new UserResponse());

            userService.registerUser(request);

            verify(professionalProfileRepository).save(professionalProfileCaptor.capture());
            assertThat(professionalProfileCaptor.getValue().getUser()).isEqualTo(user);
            verify(companyProfileRepository, never()).save(any());
            verify(eventPublisher).publishUserRegistered(any());
        }

        @Test
        void registeringACompanyDoesNotCreateAProfessionalProfile() {
            user.setRole(UserRole.COMPANY);
            UserRegistrationRequest request = new UserRegistrationRequest();
            request.setKeycloakId("kc-456");
            request.setEmail("company@example.com");
            request.setRole(UserRole.COMPANY);

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(user);
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(new UserResponse());

            userService.registerUser(request);

            verify(professionalProfileRepository, never()).save(any());
            verify(eventPublisher).publishUserRegistered(any());
        }

        @Test
        void publishedEventContainsCorrectUserIdEmailAndRole() {
            UserRegistrationRequest request = new UserRegistrationRequest();
            request.setKeycloakId("kc-123");
            request.setEmail("professional@example.com");
            request.setRole(UserRole.PROFESSIONAL);

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(user);
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(new UserResponse());

            userService.registerUser(request);

            ArgumentCaptor<com.skillmatch.userservice.event.UserRegisteredEvent> eventCaptor =
                    ArgumentCaptor.forClass(com.skillmatch.userservice.event.UserRegisteredEvent.class);
            verify(eventPublisher).publishUserRegistered(eventCaptor.capture());
            var data = eventCaptor.getValue().getData();
            assertThat(data.getUserId()).isEqualTo(userId);
            assertThat(data.getEmail()).isEqualTo("professional@example.com");
            assertThat(data.getRole()).isEqualTo("PROFESSIONAL");
        }

        @Test
        void registeringWithAnExistingEmailThrows() {
            UserRegistrationRequest request = new UserRegistrationRequest();
            request.setEmail("professional@example.com");
            request.setRole(UserRole.PROFESSIONAL);

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> userService.registerUser(request))
                    .isInstanceOf(DuplicateEmailException.class);

            verify(userRepository, never()).save(any());
            verify(eventPublisher, never()).publishUserRegistered(any());
        }
    }

    // =========================================================================
    // getUserProfile
    // =========================================================================

    @Nested
    class GetUserProfile {

        @Test
        void returnsTheMappedUserWhenFound() {
            UserResponse response = new UserResponse();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userMapper.toResponse(user)).thenReturn(response);

            UserResponse result = userService.getUserProfile(userId);

            assertThat(result).isSameAs(response);
        }

        @Test
        void throwsWhenUserDoesNotExist() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserProfile(userId))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // =========================================================================
    // updateProfessionalProfile
    // =========================================================================

    @Nested
    class UpdateProfessionalProfile {

        @Test
        void createsANewProfileWhenNoneExistsYet() {
            ProfessionalProfileRequest request = new ProfessionalProfileRequest();
            request.setFirstName("Ada");
            request.setLastName("Lovelace");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(professionalProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(professionalProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(professionalProfileMapper.toResponse(any())).thenReturn(new ProfessionalProfileResponse());

            userService.updateProfessionalProfile(userId, request);

            verify(professionalProfileMapper).updateEntity(eq(request), professionalProfileCaptor.capture());
            assertThat(professionalProfileCaptor.getValue().getUser()).isEqualTo(user);
        }

        @Test
        void updatesTheExistingProfileWhenOneIsFound() {
            ProfessionalProfileRequest request = new ProfessionalProfileRequest();
            ProfessionalProfile existing = new ProfessionalProfile();
            existing.setUser(user);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(professionalProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
            when(professionalProfileRepository.save(existing)).thenReturn(existing);
            when(professionalProfileMapper.toResponse(existing)).thenReturn(new ProfessionalProfileResponse());

            userService.updateProfessionalProfile(userId, request);

            verify(professionalProfileMapper).updateEntity(request, existing);
            verify(professionalProfileRepository).save(existing);
        }

        @Test
        void throwsWhenUserIsNotAProfessional() {
            user.setRole(UserRole.COMPANY);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.updateProfessionalProfile(userId, new ProfessionalProfileRequest()))
                    .isInstanceOf(InvalidUserOperationException.class);

            verify(professionalProfileRepository, never()).save(any());
        }

        @Test
        void throwsWhenUserDoesNotExist() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateProfessionalProfile(userId, new ProfessionalProfileRequest()))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // =========================================================================
    // updateCompanyProfile
    // =========================================================================

    @Nested
    class UpdateCompanyProfile {

        @BeforeEach
        void makeUserACompany() {
            user.setRole(UserRole.COMPANY);
        }

        @Test
        void createsANewProfileWhenNoneExistsYet() {
            CompanyProfileRequest request = new CompanyProfileRequest();
            request.setCompanyName("Acme Corp");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(companyProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(companyProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(companyProfileMapper.toResponse(any())).thenReturn(new CompanyProfileResponse());

            userService.updateCompanyProfile(userId, request);

            ArgumentCaptor<CompanyProfile> captor = ArgumentCaptor.forClass(CompanyProfile.class);
            verify(companyProfileMapper).updateEntity(eq(request), captor.capture());
            assertThat(captor.getValue().getUser()).isEqualTo(user);
        }

        @Test
        void throwsWhenUserIsNotACompany() {
            user.setRole(UserRole.PROFESSIONAL);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.updateCompanyProfile(userId, new CompanyProfileRequest()))
                    .isInstanceOf(InvalidUserOperationException.class);

            verify(companyProfileRepository, never()).save(any());
        }

        @Test
        void throwsWhenUserDoesNotExist() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateCompanyProfile(userId, new CompanyProfileRequest()))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // =========================================================================
    // searchProfessionalsBySkill
    // =========================================================================

    @Test
    void searchProfessionalsBySkillMapsEachMatchingProfile() {
        ProfessionalProfile profile = new ProfessionalProfile();
        ProfessionalProfileResponse response = new ProfessionalProfileResponse();

        when(professionalProfileRepository.findValidatedBySkillName("Java")).thenReturn(List.of(profile));
        when(professionalProfileMapper.toResponse(profile)).thenReturn(response);

        List<ProfessionalProfileResponse> result = userService.searchProfessionalsBySkill("Java");

        assertThat(result).containsExactly(response);
    }

    // =========================================================================
    // validateProfessional
    // =========================================================================

    @Nested
    class ValidateProfessional {

        @Test
        void movesAPendingProfessionalToValidatedAndPublishesEvent() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.toResponse(any())).thenReturn(new UserResponse());

            userService.validateProfessional(userId);

            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.VALIDATED);
            verify(eventPublisher).publishUserValidated(any());
        }

        @Test
        void throwsWhenUserIsNotAProfessional() {
            user.setRole(UserRole.COMPANY);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.validateProfessional(userId))
                    .isInstanceOf(InvalidUserOperationException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void throwsWhenAlreadyValidated() {
            user.setStatus(UserStatus.VALIDATED);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.validateProfessional(userId))
                    .isInstanceOf(InvalidUserOperationException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void throwsWhenUserDoesNotExist() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.validateProfessional(userId))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // =========================================================================
    // suspendUser
    // =========================================================================

    @Nested
    class SuspendUser {

        @Test
        void suspendsAPendingOrValidatedUser() {
            user.setStatus(UserStatus.VALIDATED);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.toResponse(any())).thenReturn(new UserResponse());

            userService.suspendUser(userId);

            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.SUSPENDED);
        }

        @Test
        void throwsWhenAlreadySuspended() {
            user.setStatus(UserStatus.SUSPENDED);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.suspendUser(userId))
                    .isInstanceOf(InvalidUserOperationException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void throwsWhenUserDoesNotExist() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.suspendUser(userId))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // =========================================================================
    // updateReputation
    // =========================================================================

    @Nested
    class UpdateReputation {

        @ParameterizedTest
        @CsvSource({
                "4.5, 10, TOP_PERFORMER",
                "5.0, 25, TOP_PERFORMER",
                "4.5, 9,  AFFIDABILE",
                "3.5, 3,  AFFIDABILE",
                "4.4, 100, AFFIDABILE",
                "3.5, 2,  JUNIOR",
                "1.0, 0,  JUNIOR"
        })
        void computesTheReputationLevelFromRatingAndReviewThresholds(
                String avgRating, int totalReviews, ReputationLevel expected) {
            ProfessionalProfile profile = new ProfessionalProfile();
            when(professionalProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
            when(professionalProfileRepository.save(profile)).thenReturn(profile);

            userService.updateReputation(userId, new BigDecimal(avgRating), totalReviews);

            assertThat(profile.getReputationLevel()).isEqualTo(expected);
            assertThat(profile.getAvgRating()).isEqualTo(new BigDecimal(avgRating));
            assertThat(profile.getTotalReviews()).isEqualTo(totalReviews);
        }

        @Test
        void throwsWhenNoProfessionalProfileExists() {
            when(professionalProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateReputation(userId, BigDecimal.ONE, 1))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // =========================================================================
    // listUsers
    // =========================================================================

    @Test
    void listUsersMapsEachPageElement() {
        Pageable pageable = Pageable.ofSize(20);
        Page<User> userPage = new PageImpl<>(List.of(user));
        UserResponse response = new UserResponse();

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toResponse(user)).thenReturn(response);

        Page<UserResponse> result = userService.listUsers(pageable);

        assertThat(result.getContent()).containsExactly(response);
    }
}
