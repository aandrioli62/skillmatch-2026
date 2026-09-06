package com.skillmatch.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatch.userservice.config.TestSecurityConfig;
import com.skillmatch.userservice.dto.request.ReportRequest;
import com.skillmatch.userservice.dto.response.ReportResponse;
import com.skillmatch.userservice.exception.GlobalExceptionHandler;
import com.skillmatch.userservice.exception.InvalidUserOperationException;
import com.skillmatch.userservice.exception.UserNotFoundException;
import com.skillmatch.userservice.model.enums.ReportStatus;
import com.skillmatch.userservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("ReportController — @WebMvcTest")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private ReportRequest buildRequest() {
        ReportRequest request = new ReportRequest();
        request.setReportedUserId(UUID.randomUUID());
        request.setReason("Non si è presentato al colloquio concordato.");
        return request;
    }

    @Test
    @DisplayName("PROFESSIONAL files a report → 201 Created")
    void createReport_professional_success() throws Exception {
        ReportRequest request = buildRequest();
        ReportResponse response = new ReportResponse();
        response.setReportedUserId(request.getReportedUserId());
        response.setReason(request.getReason());
        response.setStatus(ReportStatus.OPEN);

        when(userService.createReport(eq("kc-1"), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/reports")
                        .with(jwt().jwt(b -> b.subject("kc-1")).authorities(new SimpleGrantedAuthority("ROLE_PROFESSIONAL")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("COMPANY files a report → 201 Created")
    void createReport_company_success() throws Exception {
        ReportRequest request = buildRequest();
        ReportResponse response = new ReportResponse();
        response.setStatus(ReportStatus.OPEN);

        when(userService.createReport(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/reports")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_COMPANY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("ADMIN role → 403 Forbidden")
    void createReport_admin_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/reports")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("blank reason → 400 Bad Request")
    void createReport_blankReason_badRequest() throws Exception {
        ReportRequest request = buildRequest();
        request.setReason("");

        mockMvc.perform(post("/api/v1/reports")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROFESSIONAL")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("reporting yourself → 422 Unprocessable Entity")
    void createReport_self_unprocessable() throws Exception {
        when(userService.createReport(any(), any()))
                .thenThrow(new InvalidUserOperationException("You cannot report yourself."));

        mockMvc.perform(post("/api/v1/reports")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROFESSIONAL")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("reported user not found → 404 Not Found")
    void createReport_reportedUserNotFound() throws Exception {
        UUID unknown = UUID.randomUUID();
        when(userService.createReport(any(), any())).thenThrow(new UserNotFoundException(unknown));

        mockMvc.perform(post("/api/v1/reports")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROFESSIONAL")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isNotFound());
    }
}
