package com.skillmatch.paymentservice.controller;

import com.skillmatch.paymentservice.config.TestSecurityConfig;
import com.skillmatch.paymentservice.dto.response.CommissionConfigResponse;
import com.skillmatch.paymentservice.exception.GlobalExceptionHandler;
import com.skillmatch.paymentservice.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommissionConfigController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("CommissionConfigController — @WebMvcTest")
class CommissionConfigControllerTest {

    private static final SimpleGrantedAuthority ROLE_COMPANY = new SimpleGrantedAuthority("ROLE_COMPANY");
    private static final SimpleGrantedAuthority ROLE_PROFESSIONAL = new SimpleGrantedAuthority("ROLE_PROFESSIONAL");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    @DisplayName("COMPANY caller → 200 OK with current rate")
    void getCurrentCommissionConfig_companyCaller_success() throws Exception {
        CommissionConfigResponse response = new CommissionConfigResponse();
        response.setRatePercentage(BigDecimal.valueOf(10.00));
        when(paymentService.getCurrentCommissionConfig()).thenReturn(response);

        mockMvc.perform(get("/api/v1/commission-config/current").with(jwt().authorities(ROLE_COMPANY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratePercentage").value(10.00));
    }

    @Test
    @DisplayName("PROFESSIONAL caller → 200 OK with current rate")
    void getCurrentCommissionConfig_professionalCaller_success() throws Exception {
        CommissionConfigResponse response = new CommissionConfigResponse();
        response.setRatePercentage(BigDecimal.valueOf(8.00));
        when(paymentService.getCurrentCommissionConfig()).thenReturn(response);

        mockMvc.perform(get("/api/v1/commission-config/current").with(jwt().authorities(ROLE_PROFESSIONAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratePercentage").value(8.00));
    }
}
