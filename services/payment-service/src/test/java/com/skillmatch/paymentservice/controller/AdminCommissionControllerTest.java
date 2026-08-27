package com.skillmatch.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatch.paymentservice.client.UserServiceClient;
import com.skillmatch.paymentservice.config.TestSecurityConfig;
import com.skillmatch.paymentservice.dto.request.CommissionConfigRequest;
import com.skillmatch.paymentservice.dto.response.CommissionConfigResponse;
import com.skillmatch.paymentservice.exception.GlobalExceptionHandler;
import com.skillmatch.paymentservice.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCommissionController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("AdminCommissionController — @WebMvcTest")
class AdminCommissionControllerTest {

    private static final SimpleGrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");
    private static final SimpleGrantedAuthority ROLE_COMPANY = new SimpleGrantedAuthority("ROLE_COMPANY");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private UserServiceClient userServiceClient;

    @Nested
    @DisplayName("GET /api/v1/admin/commission-config")
    class GetCommissionConfig {

        @Test
        @DisplayName("ADMIN caller → 200 OK")
        void getCurrentCommissionConfig_success() throws Exception {
            CommissionConfigResponse response = new CommissionConfigResponse();
            response.setRatePercentage(BigDecimal.valueOf(8.00));
            when(paymentService.getCurrentCommissionConfig()).thenReturn(response);

            mockMvc.perform(get("/api/v1/admin/commission-config").with(jwt().authorities(ROLE_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ratePercentage").value(8.00));
        }

        @Test
        @DisplayName("COMPANY role → 403 Forbidden")
        void getCurrentCommissionConfig_wrongRole_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/admin/commission-config").with(jwt().authorities(ROLE_COMPANY)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/commission-config")
    class UpdateCommissionConfig {

        @Test
        @DisplayName("ADMIN caller with valid rate → 200 OK")
        void updateCommissionRate_success() throws Exception {
            UUID adminId = UUID.randomUUID();
            CommissionConfigRequest request = new CommissionConfigRequest();
            request.setRatePercentage(BigDecimal.valueOf(10.00));

            CommissionConfigResponse response = new CommissionConfigResponse();
            response.setRatePercentage(BigDecimal.valueOf(10.00));

            when(userServiceClient.resolveCurrentUserId()).thenReturn(adminId);
            when(paymentService.updateCommissionRate(eq(adminId), any())).thenReturn(response);

            mockMvc.perform(put("/api/v1/admin/commission-config")
                            .with(jwt().authorities(ROLE_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ratePercentage").value(10.00));
        }

        @Test
        @DisplayName("rate above 100 → 400 Bad Request")
        void updateCommissionRate_invalidRate_badRequest() throws Exception {
            CommissionConfigRequest request = new CommissionConfigRequest();
            request.setRatePercentage(BigDecimal.valueOf(150));

            mockMvc.perform(put("/api/v1/admin/commission-config")
                            .with(jwt().authorities(ROLE_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("COMPANY role → 403 Forbidden")
        void updateCommissionRate_wrongRole_forbidden() throws Exception {
            CommissionConfigRequest request = new CommissionConfigRequest();
            request.setRatePercentage(BigDecimal.valueOf(10));

            mockMvc.perform(put("/api/v1/admin/commission-config")
                            .with(jwt().authorities(ROLE_COMPANY))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}
