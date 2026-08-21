package com.skillmatch.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatch.userservice.config.SecurityConfig;
import com.skillmatch.userservice.dto.request.CompanyProfileRequest;
import com.skillmatch.userservice.dto.request.ProfessionalProfileRequest;
import com.skillmatch.userservice.dto.request.UserRegistrationRequest;
import com.skillmatch.userservice.dto.response.CompanyProfileResponse;
import com.skillmatch.userservice.dto.response.ProfessionalProfileResponse;
import com.skillmatch.userservice.dto.response.UserResponse;
import com.skillmatch.userservice.exception.DuplicateEmailException;
import com.skillmatch.userservice.exception.InvalidUserOperationException;
import com.skillmatch.userservice.exception.UserNotFoundException;
import com.skillmatch.userservice.model.enums.UserRole;
import com.skillmatch.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private static final String PROFESSIONAL_ROLE = "PROFESSIONAL";
    private static final String COMPANY_ROLE = "COMPANY";

    // =========================================================================
    // POST /api/v1/users (public)
    // =========================================================================

    @Test
    void registerUserReturns201WithoutRequiringAuthentication() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setKeycloakId("kc-1");
        request.setEmail("new@example.com");
        request.setRole(UserRole.PROFESSIONAL);

        UserResponse response = new UserResponse();
        response.setId(UUID.randomUUID());
        response.setEmail("new@example.com");

        when(userService.registerUser(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void registerUserReturns400WhenBodyFailsValidation() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("not-an-email");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.keycloakId").exists())
                .andExpect(jsonPath("$.errors.role").exists());
    }

    @Test
    void registerUserReturns409WhenEmailAlreadyExists() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setKeycloakId("kc-1");
        request.setEmail("dup@example.com");
        request.setRole(UserRole.PROFESSIONAL);

        when(userService.registerUser(any())).thenThrow(new DuplicateEmailException("dup@example.com"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // GET /api/v1/users/{userId} (authenticated)
    // =========================================================================

    @Test
    void getUserProfileReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserProfileReturns200ForAnAuthenticatedCaller() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse response = new UserResponse();
        response.setId(userId);
        when(userService.getUserProfile(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/{userId}", userId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()));
    }

    @Test
    void getUserProfileReturns404WhenNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getUserProfile(userId)).thenThrow(new UserNotFoundException(userId));

        mockMvc.perform(get("/api/v1/users/{userId}", userId).with(jwt()))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // PUT /api/v1/users/{userId}/professional-profile (ROLE_PROFESSIONAL)
    // =========================================================================

    @Test
    void updateProfessionalProfileReturns200ForAProfessionalCaller() throws Exception {
        UUID userId = UUID.randomUUID();
        ProfessionalProfileRequest request = new ProfessionalProfileRequest();
        request.setFirstName("Ada");
        request.setLastName("Lovelace");

        ProfessionalProfileResponse response = new ProfessionalProfileResponse();
        response.setUserId(userId);
        when(userService.updateProfessionalProfile(eq(userId), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/{userId}/professional-profile", userId)
                        .with(jwt().authorities(() -> "ROLE_" + PROFESSIONAL_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void updateProfessionalProfileReturns403ForANonProfessionalCaller() throws Exception {
        UUID userId = UUID.randomUUID();
        ProfessionalProfileRequest request = new ProfessionalProfileRequest();
        request.setFirstName("Ada");
        request.setLastName("Lovelace");

        mockMvc.perform(put("/api/v1/users/{userId}/professional-profile", userId)
                        .with(jwt().authorities(() -> "ROLE_" + COMPANY_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateProfessionalProfileReturns422WhenUserIsNotAProfessional() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.updateProfessionalProfile(eq(userId), any()))
                .thenThrow(new InvalidUserOperationException("not a professional"));

        ProfessionalProfileRequest request = new ProfessionalProfileRequest();
        request.setFirstName("Ada");
        request.setLastName("Lovelace");

        mockMvc.perform(put("/api/v1/users/{userId}/professional-profile", userId)
                        .with(jwt().authorities(() -> "ROLE_" + PROFESSIONAL_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    // =========================================================================
    // PUT /api/v1/users/{userId}/company-profile (ROLE_COMPANY)
    // =========================================================================

    @Test
    void updateCompanyProfileReturns200ForACompanyCaller() throws Exception {
        UUID userId = UUID.randomUUID();
        CompanyProfileRequest request = new CompanyProfileRequest();
        request.setCompanyName("Acme Corp");

        CompanyProfileResponse response = new CompanyProfileResponse();
        response.setUserId(userId);
        when(userService.updateCompanyProfile(eq(userId), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/{userId}/company-profile", userId)
                        .with(jwt().authorities(() -> "ROLE_" + COMPANY_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void updateCompanyProfileReturns403ForANonCompanyCaller() throws Exception {
        UUID userId = UUID.randomUUID();
        CompanyProfileRequest request = new CompanyProfileRequest();
        request.setCompanyName("Acme Corp");

        mockMvc.perform(put("/api/v1/users/{userId}/company-profile", userId)
                        .with(jwt().authorities(() -> "ROLE_" + PROFESSIONAL_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCompanyProfileReturns400WhenCompanyNameIsBlank() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/users/{userId}/company-profile", userId)
                        .with(jwt().authorities(() -> "ROLE_" + COMPANY_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CompanyProfileRequest())))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // GET /api/v1/users/professionals/search (authenticated)
    // =========================================================================

    @Test
    void searchProfessionalsBySkillReturns200WithMatches() throws Exception {
        ProfessionalProfileResponse response = new ProfessionalProfileResponse();
        when(userService.searchProfessionalsBySkill("Java")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/users/professionals/search")
                        .param("skill", "Java")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void searchProfessionalsBySkillReturns400WhenSkillIsBlank() throws Exception {
        mockMvc.perform(get("/api/v1/users/professionals/search")
                        .param("skill", "")
                        .with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchProfessionalsBySkillReturns400WhenSkillParamIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/users/professionals/search")
                        .with(jwt()))
                .andExpect(status().isBadRequest());
    }
}
