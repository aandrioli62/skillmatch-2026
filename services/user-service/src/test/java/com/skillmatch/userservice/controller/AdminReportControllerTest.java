package com.skillmatch.userservice.controller;

import com.skillmatch.userservice.config.TestSecurityConfig;
import com.skillmatch.userservice.exception.GlobalExceptionHandler;
import com.skillmatch.userservice.exception.ReportNotFoundException;
import com.skillmatch.userservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminReportController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("AdminReportController — @WebMvcTest")
class AdminReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("ADMIN closes a report → 200 OK")
    void closeReport_admin_ok() throws Exception {
        UUID reportId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/admin/reports/{reportId}/close", reportId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        verify(userService).closeReport(reportId);
    }

    @Test
    @DisplayName("report not found → 404 Not Found")
    void closeReport_notFound() throws Exception {
        UUID reportId = UUID.randomUUID();
        doThrow(new ReportNotFoundException(reportId)).when(userService).closeReport(reportId);

        mockMvc.perform(post("/api/v1/admin/reports/{reportId}/close", reportId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("non-ADMIN role → 403 Forbidden")
    void closeReport_nonAdmin_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/reports/{reportId}/close", UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROFESSIONAL"))))
                .andExpect(status().isForbidden());
    }
}
