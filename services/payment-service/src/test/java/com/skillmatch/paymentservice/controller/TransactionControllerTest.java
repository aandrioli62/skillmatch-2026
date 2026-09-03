package com.skillmatch.paymentservice.controller;

import com.skillmatch.paymentservice.client.UserServiceClient;
import com.skillmatch.paymentservice.config.TestSecurityConfig;
import com.skillmatch.paymentservice.dto.response.InvoiceResponse;
import com.skillmatch.paymentservice.dto.response.TransactionResponse;
import com.skillmatch.paymentservice.exception.GlobalExceptionHandler;
import com.skillmatch.paymentservice.exception.InvoiceNotFoundException;
import com.skillmatch.paymentservice.exception.TransactionNotFoundException;
import com.skillmatch.paymentservice.service.PaymentService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("TransactionController — @WebMvcTest")
class TransactionControllerTest {

    private static final SimpleGrantedAuthority ROLE_COMPANY = new SimpleGrantedAuthority("ROLE_COMPANY");
    private static final SimpleGrantedAuthority ROLE_PROFESSIONAL = new SimpleGrantedAuthority("ROLE_PROFESSIONAL");
    private static final SimpleGrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private UserServiceClient userServiceClient;

    @Nested
    @DisplayName("GET /api/v1/transactions/{transactionId}")
    class GetTransaction {

        @Test
        @DisplayName("caller is a party → 200 OK")
        void getTransaction_party_ok() throws Exception {
            UUID callerId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            when(userServiceClient.resolveCurrentUserId()).thenReturn(callerId);
            when(paymentService.getTransaction(transactionId, callerId, false)).thenReturn(new TransactionResponse());

            mockMvc.perform(get("/api/v1/transactions/{transactionId}", transactionId)
                            .with(jwt().authorities(ROLE_COMPANY)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("admin caller → 200 OK, isAdmin=true propagated")
        void getTransaction_admin_ok() throws Exception {
            UUID callerId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            when(userServiceClient.resolveCurrentUserId()).thenReturn(callerId);
            when(paymentService.getTransaction(transactionId, callerId, true)).thenReturn(new TransactionResponse());

            mockMvc.perform(get("/api/v1/transactions/{transactionId}", transactionId)
                            .with(jwt().authorities(ROLE_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("caller is not a party → 403 Forbidden")
        void getTransaction_notAParty_forbidden() throws Exception {
            UUID callerId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            when(userServiceClient.resolveCurrentUserId()).thenReturn(callerId);
            when(paymentService.getTransaction(transactionId, callerId, false))
                    .thenThrow(new AccessDeniedException("not a party"));

            mockMvc.perform(get("/api/v1/transactions/{transactionId}", transactionId)
                            .with(jwt().authorities(ROLE_PROFESSIONAL)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("unknown transaction → 404 Not Found")
        void getTransaction_notFound() throws Exception {
            UUID callerId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            when(userServiceClient.resolveCurrentUserId()).thenReturn(callerId);
            when(paymentService.getTransaction(transactionId, callerId, false))
                    .thenThrow(new TransactionNotFoundException(transactionId));

            mockMvc.perform(get("/api/v1/transactions/{transactionId}", transactionId)
                            .with(jwt().authorities(ROLE_COMPANY)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/transactions/admin/all")
    class ListAllTransactions {

        @Test
        @DisplayName("ADMIN role → 200 OK")
        void listAllTransactions_admin_ok() throws Exception {
            Page<TransactionResponse> page = new PageImpl<>(List.of(new TransactionResponse()), PageRequest.of(0, 20), 1);
            when(paymentService.listAllTransactions(org.mockito.ArgumentMatchers.any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/transactions/admin/all")
                            .with(jwt().authorities(ROLE_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("non-ADMIN role → 403 Forbidden")
        void listAllTransactions_nonAdmin_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/transactions/admin/all")
                            .with(jwt().authorities(ROLE_COMPANY)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/transactions/{transactionId}/invoice")
    class GetInvoice {

        @Test
        @DisplayName("caller is a party → 200 OK")
        void getInvoice_party_ok() throws Exception {
            UUID callerId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            when(userServiceClient.resolveCurrentUserId()).thenReturn(callerId);
            when(paymentService.getInvoiceByTransaction(transactionId, callerId, false))
                    .thenReturn(new InvoiceResponse());

            mockMvc.perform(get("/api/v1/transactions/{transactionId}/invoice", transactionId)
                            .with(jwt().authorities(ROLE_COMPANY)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("caller is not a party → 403 Forbidden")
        void getInvoice_notAParty_forbidden() throws Exception {
            UUID callerId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            when(userServiceClient.resolveCurrentUserId()).thenReturn(callerId);
            when(paymentService.getInvoiceByTransaction(transactionId, callerId, false))
                    .thenThrow(new AccessDeniedException("not a party"));

            mockMvc.perform(get("/api/v1/transactions/{transactionId}/invoice", transactionId)
                            .with(jwt().authorities(ROLE_PROFESSIONAL)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("unknown invoice → 404 Not Found")
        void getInvoice_notFound() throws Exception {
            UUID callerId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            when(userServiceClient.resolveCurrentUserId()).thenReturn(callerId);
            when(paymentService.getInvoiceByTransaction(transactionId, callerId, false))
                    .thenThrow(new InvoiceNotFoundException(transactionId));

            mockMvc.perform(get("/api/v1/transactions/{transactionId}/invoice", transactionId)
                            .with(jwt().authorities(ROLE_COMPANY)))
                    .andExpect(status().isNotFound());
        }
    }
}
