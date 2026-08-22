package com.skillmatch.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatch.userservice.dto.request.CompanyProfileRequest;
import com.skillmatch.userservice.dto.request.ProfessionalProfileRequest;
import com.skillmatch.userservice.dto.request.UserRegistrationRequest;
import com.skillmatch.userservice.dto.response.CompanyProfileResponse;
import com.skillmatch.userservice.dto.response.ProfessionalProfileResponse;
import com.skillmatch.userservice.dto.response.UserResponse;
import com.skillmatch.userservice.exception.DuplicateEmailException;
import com.skillmatch.userservice.config.TestSecurityConfig;
import com.skillmatch.userservice.exception.GlobalExceptionHandler;
import com.skillmatch.userservice.exception.InvalidUserOperationException;
import com.skillmatch.userservice.exception.UserNotFoundException;
import com.skillmatch.userservice.model.enums.UserRole;
import com.skillmatch.userservice.model.enums.UserStatus;
import com.skillmatch.userservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("UserController — @WebMvcTest")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    // =========================================================================
    // POST /api/v1/users  — registerUser
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/users")
    class RegisterUser {

        @Test
        @DisplayName("valid request → 201 Created with user body")
        void registerUser_success() throws Exception {
            UserRegistrationRequest request = buildRegistrationRequest("mario@example.com", "kc-001", UserRole.PROFESSIONAL);
            UserResponse response = buildUserResponse(UserRole.PROFESSIONAL, UserStatus.PENDING);

            when(userService.registerUser(any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("mario@example.com"))
                    .andExpect(jsonPath("$.role").value("PROFESSIONAL"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("duplicate email → 409 Conflict")
        void registerUser_duplicateEmail_conflict() throws Exception {
            UserRegistrationRequest request = buildRegistrationRequest("dup@example.com", "kc-002", UserRole.PROFESSIONAL);

            when(userService.registerUser(any())).thenThrow(new DuplicateEmailException("dup@example.com"));

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Duplicate Email"));
        }

        @Test
        @DisplayName("missing email → 400 Bad Request with validation error")
        void registerUser_missingEmail_badRequest() throws Exception {
            UserRegistrationRequest request = new UserRegistrationRequest();
            request.setKeycloakId("kc-003");
            request.setRole(UserRole.PROFESSIONAL);
            // email intentionally omitted

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("invalid email format → 400 Bad Request")
        void registerUser_invalidEmail_badRequest() throws Exception {
            UserRegistrationRequest request = buildRegistrationRequest("not-an-email", "kc-004", UserRole.PROFESSIONAL);

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("missing role → 400 Bad Request")
        void registerUser_missingRole_badRequest() throws Exception {
            UserRegistrationRequest request = new UserRegistrationRequest();
            request.setEmail("test@example.com");
            request.setKeycloakId("kc-005");
            // role intentionally omitted

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // GET /api/v1/users/{userId}  — getUserProfile
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/users/{userId}")
    class GetUserProfile {

        @Test
        @DisplayName("existing user → 200 OK")
        @WithMockUser
        void getUserProfile_found() throws Exception {
            UUID userId = UUID.randomUUID();
            UserResponse response = buildUserResponse(UserRole.PROFESSIONAL, UserStatus.PENDING);
            response.setId(userId);

            when(userService.getUserProfile(userId)).thenReturn(response);

            mockMvc.perform(get("/api/v1/users/{userId}", userId)
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId.toString()));
        }

        @Test
        @DisplayName("unknown user → 404 Not Found")
        void getUserProfile_notFound() throws Exception {
            UUID unknown = UUID.randomUUID();
            when(userService.getUserProfile(unknown)).thenThrow(new UserNotFoundException(unknown));

            mockMvc.perform(get("/api/v1/users/{userId}", unknown)
                            .with(jwt()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("User Not Found"));
        }
    }

    // =========================================================================
    // PUT /api/v1/users/{userId}/professional-profile
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/v1/users/{userId}/professional-profile")
    class UpdateProfessionalProfile {

        @Test
        @DisplayName("valid request by PROFESSIONAL → 200 OK")
        void updateProfessionalProfile_success() throws Exception {
            UUID userId = UUID.randomUUID();
            ProfessionalProfileRequest request = new ProfessionalProfileRequest();
            request.setFirstName("Mario");
            request.setLastName("Rossi");

            ProfessionalProfileResponse response = new ProfessionalProfileResponse();
            response.setUserId(userId);
            response.setFirstName("Mario");
            response.setLastName("Rossi");

            when(userService.updateProfessionalProfile(eq(userId), any())).thenReturn(response);

            mockMvc.perform(put("/api/v1/users/{userId}/professional-profile", userId)
                            .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PROFESSIONAL")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Mario"))
                    .andExpect(jsonPath("$.lastName").value("Rossi"));
        }

        @Test
        @DisplayName("COMPANY role → 403 Forbidden")
        void updateProfessionalProfile_wrongRole_forbidden() throws Exception {
            UUID userId = UUID.randomUUID();
            ProfessionalProfileRequest request = new ProfessionalProfileRequest();
            request.setFirstName("X");
            request.setLastName("Y");

            mockMvc.perform(put("/api/v1/users/{userId}/professional-profile", userId)
                            .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_COMPANY")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("missing firstName → 400 Bad Request")
        void updateProfessionalProfile_missingFirstName_badRequest() throws Exception {
            UUID userId = UUID.randomUUID();
            ProfessionalProfileRequest request = new ProfessionalProfileRequest();
            request.setLastName("Rossi");
            // firstName omitted

            mockMvc.perform(put("/api/v1/users/{userId}/professional-profile", userId)
                            .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PROFESSIONAL")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("user not a PROFESSIONAL → 422 Unprocessable Entity")
        void updateProfessionalProfile_invalidOperation() throws Exception {
            UUID userId = UUID.randomUUID();
            ProfessionalProfileRequest request = new ProfessionalProfileRequest();
            request.setFirstName("X");
            request.setLastName("Y");

            when(userService.updateProfessionalProfile(eq(userId), any()))
                    .thenThrow(new InvalidUserOperationException("not a PROFESSIONAL"));

            mockMvc.perform(put("/api/v1/users/{userId}/professional-profile", userId)
                            .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PROFESSIONAL")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.title").value("Invalid User Operation"));
        }
    }

    // =========================================================================
    // PUT /api/v1/users/{userId}/company-profile
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/v1/users/{userId}/company-profile")
    class UpdateCompanyProfile {

        @Test
        @DisplayName("valid request by COMPANY → 200 OK")
        void updateCompanyProfile_success() throws Exception {
            UUID userId = UUID.randomUUID();
            CompanyProfileRequest request = new CompanyProfileRequest();
            request.setCompanyName("Acme Srl");

            CompanyProfileResponse response = new CompanyProfileResponse();
            response.setUserId(userId);
            response.setCompanyName("Acme Srl");

            when(userService.updateCompanyProfile(eq(userId), any())).thenReturn(response);

            mockMvc.perform(put("/api/v1/users/{userId}/company-profile", userId)
                            .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_COMPANY")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.companyName").value("Acme Srl"));
        }

        @Test
        @DisplayName("PROFESSIONAL role → 403 Forbidden")
        void updateCompanyProfile_wrongRole_forbidden() throws Exception {
            UUID userId = UUID.randomUUID();
            CompanyProfileRequest request = new CompanyProfileRequest();
            request.setCompanyName("Acme");

            mockMvc.perform(put("/api/v1/users/{userId}/company-profile", userId)
                            .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PROFESSIONAL")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("missing companyName → 400 Bad Request")
        void updateCompanyProfile_missingCompanyName_badRequest() throws Exception {
            UUID userId = UUID.randomUUID();
            CompanyProfileRequest request = new CompanyProfileRequest();
            // companyName omitted

            mockMvc.perform(put("/api/v1/users/{userId}/company-profile", userId)
                            .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_COMPANY")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // GET /api/v1/users/professionals/search?skill=...
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/users/professionals/search")
    class SearchProfessionalsBySkill {

        @Test
        @DisplayName("matching skill → 200 OK with list")
        void search_found() throws Exception {
            ProfessionalProfileResponse response = new ProfessionalProfileResponse();
            response.setFirstName("Mario");

            when(userService.searchProfessionalsBySkill("Java")).thenReturn(List.of(response));

            mockMvc.perform(get("/api/v1/users/professionals/search")
                            .param("skill", "Java")
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].firstName").value("Mario"));
        }

        @Test
        @DisplayName("no match → 200 OK with empty list")
        void search_empty() throws Exception {
            when(userService.searchProfessionalsBySkill("Cobol")).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/users/professionals/search")
                            .param("skill", "Cobol")
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("missing skill param → 400 Bad Request")
        void search_missingParam_badRequest() throws Exception {
            mockMvc.perform(get("/api/v1/users/professionals/search")
                            .with(jwt()))
                    .andExpect(status().isBadRequest());
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

    private UserResponse buildUserResponse(UserRole role, UserStatus status) {
        UserResponse res = new UserResponse();
        res.setId(UUID.randomUUID());
        res.setEmail("mario@example.com");
        res.setRole(role);
        res.setStatus(status);
        return res;
    }
}
