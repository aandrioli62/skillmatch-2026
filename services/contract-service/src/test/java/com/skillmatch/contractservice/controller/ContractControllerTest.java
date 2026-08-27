package com.skillmatch.contractservice.controller;

import com.skillmatch.contractservice.client.UserServiceClient;
import com.skillmatch.contractservice.config.TestSecurityConfig;
import com.skillmatch.contractservice.dto.response.ContractResponse;
import com.skillmatch.contractservice.exception.ContractNotFoundException;
import com.skillmatch.contractservice.exception.GlobalExceptionHandler;
import com.skillmatch.contractservice.exception.InvalidContractOperationException;
import com.skillmatch.contractservice.model.enums.ContractStatus;
import com.skillmatch.contractservice.service.ContractService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContractController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("ContractController — @WebMvcTest")
class ContractControllerTest {

    private static final SimpleGrantedAuthority ROLE_COMPANY = new SimpleGrantedAuthority("ROLE_COMPANY");
    private static final SimpleGrantedAuthority ROLE_PROFESSIONAL = new SimpleGrantedAuthority("ROLE_PROFESSIONAL");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContractService contractService;

    @MockBean
    private UserServiceClient userServiceClient;

    // =========================================================================
    // GET /api/v1/contracts/{contractId}
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/contracts/{contractId}")
    class GetContract {

        @Test
        @DisplayName("existing contract → 200 OK")
        void getContract_found() throws Exception {
            UUID contractId = UUID.randomUUID();
            ContractResponse response = new ContractResponse();
            response.setId(contractId);

            when(contractService.getContract(contractId)).thenReturn(response);

            mockMvc.perform(get("/api/v1/contracts/{contractId}", contractId).with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(contractId.toString()));
        }

        @Test
        @DisplayName("unknown contract → 404 Not Found")
        void getContract_notFound() throws Exception {
            UUID contractId = UUID.randomUUID();
            when(contractService.getContract(contractId)).thenThrow(new ContractNotFoundException(contractId));

            mockMvc.perform(get("/api/v1/contracts/{contractId}", contractId).with(jwt()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Contract Not Found"));
        }

        @Test
        @DisplayName("no token → 401 Unauthorized")
        void getContract_noToken_unauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/contracts/{contractId}", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // GET /api/v1/contracts/company/mine and /professional/mine
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/contracts/company/mine")
    class ListMyContractsAsCompany {

        @Test
        @DisplayName("COMPANY caller → 200 OK with own contracts")
        void listMyContractsAsCompany_success() throws Exception {
            UUID companyId = UUID.randomUUID();
            when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
            when(contractService.listContractsByCompany(companyId)).thenReturn(List.of(new ContractResponse()));

            mockMvc.perform(get("/api/v1/contracts/company/mine").with(jwt().authorities(ROLE_COMPANY)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("PROFESSIONAL role → 403 Forbidden")
        void listMyContractsAsCompany_wrongRole_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/contracts/company/mine").with(jwt().authorities(ROLE_PROFESSIONAL)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/contracts/professional/mine")
    class ListMyContractsAsProfessional {

        @Test
        @DisplayName("PROFESSIONAL caller → 200 OK with own contracts")
        void listMyContractsAsProfessional_success() throws Exception {
            UUID professionalId = UUID.randomUUID();
            when(userServiceClient.resolveCurrentUserId()).thenReturn(professionalId);
            when(contractService.listContractsByProfessional(professionalId)).thenReturn(List.of(new ContractResponse()));

            mockMvc.perform(get("/api/v1/contracts/professional/mine").with(jwt().authorities(ROLE_PROFESSIONAL)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    // =========================================================================
    // PUT /api/v1/contracts/{contractId}/sign
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/v1/contracts/{contractId}/sign")
    class SignContract {

        @Test
        @DisplayName("company signs first → 200 OK")
        void signContract_success() throws Exception {
            UUID companyId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            ContractResponse response = new ContractResponse();
            response.setStatus(ContractStatus.PENDING_SIGNATURES);

            when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
            when(contractService.signContract(companyId, contractId)).thenReturn(response);

            mockMvc.perform(put("/api/v1/contracts/{contractId}/sign", contractId)
                            .with(jwt().authorities(ROLE_COMPANY)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING_SIGNATURES"));
        }

        @Test
        @DisplayName("wrong party for current stage → 422 Unprocessable Entity")
        void signContract_wrongParty_unprocessable() throws Exception {
            UUID professionalId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();

            when(userServiceClient.resolveCurrentUserId()).thenReturn(professionalId);
            when(contractService.signContract(professionalId, contractId))
                    .thenThrow(new InvalidContractOperationException("awaiting the company's signature"));

            mockMvc.perform(put("/api/v1/contracts/{contractId}/sign", contractId)
                            .with(jwt().authorities(ROLE_PROFESSIONAL)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("ADMIN role → 403 Forbidden")
        void signContract_adminRole_forbidden() throws Exception {
            mockMvc.perform(put("/api/v1/contracts/{contractId}/sign", UUID.randomUUID())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // PUT /api/v1/contracts/{contractId}/complete
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/v1/contracts/{contractId}/complete")
    class CompleteContract {

        @Test
        @DisplayName("owning company → 200 OK")
        void completeContract_success() throws Exception {
            UUID companyId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            ContractResponse response = new ContractResponse();
            response.setStatus(ContractStatus.COMPLETED);

            when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
            when(contractService.completeContract(companyId, contractId)).thenReturn(response);

            mockMvc.perform(put("/api/v1/contracts/{contractId}/complete", contractId)
                            .with(jwt().authorities(ROLE_COMPANY)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("PROFESSIONAL role → 403 Forbidden")
        void completeContract_wrongRole_forbidden() throws Exception {
            mockMvc.perform(put("/api/v1/contracts/{contractId}/complete", UUID.randomUUID())
                            .with(jwt().authorities(ROLE_PROFESSIONAL)))
                    .andExpect(status().isForbidden());
        }
    }
}
