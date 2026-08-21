package com.skillmatch.userservice.controller;

import com.skillmatch.userservice.config.SecurityConfig;
import com.skillmatch.userservice.dto.response.UserResponse;
import com.skillmatch.userservice.exception.InvalidUserOperationException;
import com.skillmatch.userservice.exception.UserNotFoundException;
import com.skillmatch.userservice.model.enums.UserStatus;
import com.skillmatch.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@Import(SecurityConfig.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private static final org.springframework.security.core.GrantedAuthority ADMIN = () -> "ROLE_ADMIN";
    private static final org.springframework.security.core.GrantedAuthority PROFESSIONAL = () -> "ROLE_PROFESSIONAL";

    // =========================================================================
    // GET /api/v1/admin/users
    // =========================================================================

    @Test
    void listUsersReturns200ForAnAdminCaller() throws Exception {
        Page<UserResponse> page = new PageImpl<>(List.of(new UserResponse()));
        when(userService.listUsers(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/users").with(jwt().authorities(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void listUsersReturns403ForANonAdminCaller() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").with(jwt().authorities(PROFESSIONAL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsersReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // POST /api/v1/admin/users/{userId}/validate
    // =========================================================================

    @Test
    void validateProfessionalReturns200ForAnAdminCaller() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse response = new UserResponse();
        response.setId(userId);
        response.setStatus(UserStatus.VALIDATED);
        when(userService.validateProfessional(userId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/users/{userId}/validate", userId).with(jwt().authorities(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"));
    }

    @Test
    void validateProfessionalReturns403ForANonAdminCaller() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/{userId}/validate", UUID.randomUUID())
                        .with(jwt().authorities(PROFESSIONAL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void validateProfessionalReturns404WhenUserDoesNotExist() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.validateProfessional(userId)).thenThrow(new UserNotFoundException(userId));

        mockMvc.perform(post("/api/v1/admin/users/{userId}/validate", userId).with(jwt().authorities(ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void validateProfessionalReturns422WhenAlreadyValidated() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.validateProfessional(userId))
                .thenThrow(new InvalidUserOperationException("already validated"));

        mockMvc.perform(post("/api/v1/admin/users/{userId}/validate", userId).with(jwt().authorities(ADMIN)))
                .andExpect(status().isUnprocessableEntity());
    }

    // =========================================================================
    // POST /api/v1/admin/users/{userId}/suspend
    // =========================================================================

    @Test
    void suspendUserReturns200ForAnAdminCaller() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse response = new UserResponse();
        response.setId(userId);
        response.setStatus(UserStatus.SUSPENDED);
        when(userService.suspendUser(userId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/users/{userId}/suspend", userId).with(jwt().authorities(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void suspendUserReturns403ForANonAdminCaller() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/{userId}/suspend", UUID.randomUUID())
                        .with(jwt().authorities(PROFESSIONAL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void suspendUserReturns422WhenAlreadySuspended() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.suspendUser(userId)).thenThrow(new InvalidUserOperationException("already suspended"));

        mockMvc.perform(post("/api/v1/admin/users/{userId}/suspend", userId).with(jwt().authorities(ADMIN)))
                .andExpect(status().isUnprocessableEntity());
    }
}
