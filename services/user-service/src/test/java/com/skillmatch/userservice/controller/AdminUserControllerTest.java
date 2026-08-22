package com.skillmatch.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatch.userservice.dto.response.UserResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

@WebMvcTest(AdminUserController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("AdminUserController — @WebMvcTest")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    // =========================================================================
    // GET /api/v1/admin/users — listUsers
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/admin/users")
    class ListUsers {

        @Test
        @DisplayName("ADMIN role → 200 OK with paginated users")
        void listUsers_admin_ok() throws Exception {
            UserResponse u = buildUserResponse(UserRole.PROFESSIONAL, UserStatus.PENDING);
            Page<UserResponse> page = new PageImpl<>(List.of(u), PageRequest.of(0, 20), 1);

            when(userService.listUsers(any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/admin/users")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].role").value("PROFESSIONAL"));
        }

        @Test
        @DisplayName("no authentication → 401 Unauthorized")
        void listUsers_unauthenticated_401() throws Exception {
            mockMvc.perform(get("/api/v1/admin/users"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("non-ADMIN role → 403 Forbidden")
        void listUsers_nonAdmin_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/admin/users")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROFESSIONAL"))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // POST /api/v1/admin/users/{userId}/validate
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/admin/users/{userId}/validate")
    class ValidateProfessional {

        @Test
        @DisplayName("ADMIN validates pending professional → 200 OK")
        void validateProfessional_admin_ok() throws Exception {
            UUID userId = UUID.randomUUID();
            UserResponse validated = buildUserResponse(UserRole.PROFESSIONAL, UserStatus.VALIDATED);
            validated.setId(userId);

            when(userService.validateProfessional(userId)).thenReturn(validated);

            mockMvc.perform(post("/api/v1/admin/users/{userId}/validate", userId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("VALIDATED"))
                    .andExpect(jsonPath("$.id").value(userId.toString()));
        }

        @Test
        @DisplayName("user not found → 404 Not Found")
        void validateProfessional_notFound() throws Exception {
            UUID unknown = UUID.randomUUID();
            when(userService.validateProfessional(unknown)).thenThrow(new UserNotFoundException(unknown));

            mockMvc.perform(post("/api/v1/admin/users/{userId}/validate", unknown)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("User Not Found"));
        }

        @Test
        @DisplayName("already validated → 422 Unprocessable Entity")
        void validateProfessional_alreadyValidated() throws Exception {
            UUID userId = UUID.randomUUID();
            when(userService.validateProfessional(userId))
                    .thenThrow(new InvalidUserOperationException("already in VALIDATED status"));

            mockMvc.perform(post("/api/v1/admin/users/{userId}/validate", userId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.title").value("Invalid User Operation"));
        }

        @Test
        @DisplayName("non-ADMIN role → 403 Forbidden")
        void validateProfessional_nonAdmin_forbidden() throws Exception {
            UUID userId = UUID.randomUUID();

            mockMvc.perform(post("/api/v1/admin/users/{userId}/validate", userId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_COMPANY"))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // POST /api/v1/admin/users/{userId}/suspend
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/admin/users/{userId}/suspend")
    class SuspendUser {

        @Test
        @DisplayName("ADMIN suspends active user → 200 OK")
        void suspendUser_admin_ok() throws Exception {
            UUID userId = UUID.randomUUID();
            UserResponse suspended = buildUserResponse(UserRole.PROFESSIONAL, UserStatus.SUSPENDED);
            suspended.setId(userId);

            when(userService.suspendUser(userId)).thenReturn(suspended);

            mockMvc.perform(post("/api/v1/admin/users/{userId}/suspend", userId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUSPENDED"));
        }

        @Test
        @DisplayName("user not found → 404 Not Found")
        void suspendUser_notFound() throws Exception {
            UUID unknown = UUID.randomUUID();
            when(userService.suspendUser(unknown)).thenThrow(new UserNotFoundException(unknown));

            mockMvc.perform(post("/api/v1/admin/users/{userId}/suspend", unknown)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("already suspended → 422 Unprocessable Entity")
        void suspendUser_alreadySuspended() throws Exception {
            UUID userId = UUID.randomUUID();
            when(userService.suspendUser(userId))
                    .thenThrow(new InvalidUserOperationException("already SUSPENDED"));

            mockMvc.perform(post("/api/v1/admin/users/{userId}/suspend", userId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("non-ADMIN role → 403 Forbidden")
        void suspendUser_nonAdmin_forbidden() throws Exception {
            UUID userId = UUID.randomUUID();

            mockMvc.perform(post("/api/v1/admin/users/{userId}/suspend", userId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROFESSIONAL"))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private UserResponse buildUserResponse(UserRole role, UserStatus status) {
        UserResponse res = new UserResponse();
        res.setId(UUID.randomUUID());
        res.setEmail("user@example.com");
        res.setRole(role);
        res.setStatus(status);
        return res;
    }
}
