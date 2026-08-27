package com.skillmatch.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatch.paymentservice.client.UserServiceClient;
import com.skillmatch.paymentservice.config.TestSecurityConfig;
import com.skillmatch.paymentservice.dto.request.InitiatePaymentRequest;
import com.skillmatch.paymentservice.dto.response.TransactionResponse;
import com.skillmatch.paymentservice.exception.GlobalExceptionHandler;
import com.skillmatch.paymentservice.exception.InvalidPaymentOperationException;
import com.skillmatch.paymentservice.model.enums.TransactionStatus;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("PaymentController — @WebMvcTest")
class PaymentControllerTest {

    private static final SimpleGrantedAuthority ROLE_COMPANY = new SimpleGrantedAuthority("ROLE_COMPANY");
    private static final SimpleGrantedAuthority ROLE_PROFESSIONAL = new SimpleGrantedAuthority("ROLE_PROFESSIONAL");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private UserServiceClient userServiceClient;

    @Nested
    @DisplayName("POST /api/v1/payments")
    class InitiatePayment {

        @Test
        @DisplayName("COMPANY caller → 201 Created")
        void initiatePayment_success() throws Exception {
            UUID companyId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            InitiatePaymentRequest request = new InitiatePaymentRequest();
            request.setContractId(contractId);

            TransactionResponse response = new TransactionResponse();
            response.setStatus(TransactionStatus.COMPLETED);

            when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
            when(paymentService.initiatePayment(eq(companyId), eq(contractId))).thenReturn(response);

            mockMvc.perform(post("/api/v1/payments")
                            .with(jwt().authorities(ROLE_COMPANY))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("PROFESSIONAL role → 403 Forbidden")
        void initiatePayment_wrongRole_forbidden() throws Exception {
            InitiatePaymentRequest request = new InitiatePaymentRequest();
            request.setContractId(UUID.randomUUID());

            mockMvc.perform(post("/api/v1/payments")
                            .with(jwt().authorities(ROLE_PROFESSIONAL))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("no token → 401 Unauthorized")
        void initiatePayment_noToken_unauthorized() throws Exception {
            InitiatePaymentRequest request = new InitiatePaymentRequest();
            request.setContractId(UUID.randomUUID());

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("missing contractId → 400 Bad Request")
        void initiatePayment_missingContractId_badRequest() throws Exception {
            InitiatePaymentRequest request = new InitiatePaymentRequest();

            mockMvc.perform(post("/api/v1/payments")
                            .with(jwt().authorities(ROLE_COMPANY))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("contract already paid → 422 Unprocessable Entity")
        void initiatePayment_alreadyPaid_unprocessable() throws Exception {
            UUID companyId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            InitiatePaymentRequest request = new InitiatePaymentRequest();
            request.setContractId(contractId);

            when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
            when(paymentService.initiatePayment(companyId, contractId))
                    .thenThrow(new InvalidPaymentOperationException("has already been paid"));

            mockMvc.perform(post("/api/v1/payments")
                            .with(jwt().authorities(ROLE_COMPANY))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
