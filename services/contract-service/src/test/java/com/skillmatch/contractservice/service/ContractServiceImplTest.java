package com.skillmatch.contractservice.service;

import com.skillmatch.contractservice.dto.response.ContractResponse;
import com.skillmatch.contractservice.event.CandidatureAcceptedEvent;
import com.skillmatch.contractservice.exception.ContractNotFoundException;
import com.skillmatch.contractservice.exception.InvalidContractOperationException;
import com.skillmatch.contractservice.mapper.ContractMapper;
import com.skillmatch.contractservice.model.Contract;
import com.skillmatch.contractservice.model.enums.ContractStatus;
import com.skillmatch.contractservice.repository.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContractServiceImpl — Unit Tests")
class ContractServiceImplTest {

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private ContractMapper contractMapper;

    @InjectMocks
    private ContractServiceImpl contractService;

    private UUID projectId;
    private UUID professionalId;
    private UUID companyId;
    private UUID contractId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        professionalId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        contractId = UUID.randomUUID();
    }

    // =========================================================================
    // createFromCandidatureAccepted
    // =========================================================================

    @Nested
    @DisplayName("createFromCandidatureAccepted()")
    class CreateFromCandidatureAccepted {

        private CandidatureAcceptedEvent.Data buildData() {
            CandidatureAcceptedEvent.Data data = new CandidatureAcceptedEvent.Data();
            data.setCandidatureId(UUID.randomUUID());
            data.setProjectId(projectId);
            data.setProfessionalId(professionalId);
            data.setCompanyId(companyId);
            data.setAmount(BigDecimal.valueOf(1500));
            return data;
        }

        @Test
        @DisplayName("no existing contract for project: saves a DRAFT contract with the default commission rate")
        void createFromCandidatureAccepted_success() {
            when(contractRepository.existsByProjectId(projectId)).thenReturn(false);
            when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

            contractService.createFromCandidatureAccepted(buildData());

            ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
            verify(contractRepository).save(captor.capture());
            Contract saved = captor.getValue();
            assertThat(saved.getProjectId()).isEqualTo(projectId);
            assertThat(saved.getProfessionalId()).isEqualTo(professionalId);
            assertThat(saved.getCompanyId()).isEqualTo(companyId);
            assertThat(saved.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
            assertThat(saved.getCommissionRate()).isEqualByComparingTo(new BigDecimal("8.00"));
            assertThat(saved.getStatus()).isEqualTo(ContractStatus.DRAFT);
        }

        @Test
        @DisplayName("contract already exists for project: skips without saving (idempotent)")
        void createFromCandidatureAccepted_duplicate_skipped() {
            when(contractRepository.existsByProjectId(projectId)).thenReturn(true);

            contractService.createFromCandidatureAccepted(buildData());

            verify(contractRepository, never()).save(any());
        }

        @Test
        @DisplayName("event carries no amount: skips without saving")
        void createFromCandidatureAccepted_noAmount_skipped() {
            when(contractRepository.existsByProjectId(projectId)).thenReturn(false);
            CandidatureAcceptedEvent.Data data = buildData();
            data.setAmount(null);

            contractService.createFromCandidatureAccepted(data);

            verify(contractRepository, never()).save(any());
        }
    }

    // =========================================================================
    // signContract
    // =========================================================================

    @Nested
    @DisplayName("signContract()")
    class SignContract {

        private Contract draftContract;

        @BeforeEach
        void setUpContract() {
            draftContract = new Contract();
            draftContract.setId(contractId);
            draftContract.setCompanyId(companyId);
            draftContract.setProfessionalId(professionalId);
            draftContract.setStatus(ContractStatus.DRAFT);
        }

        @Test
        @DisplayName("DRAFT, company signs: transitions to PENDING_SIGNATURES")
        void signContract_companySignsFirst_success() {
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
            when(contractRepository.save(draftContract)).thenReturn(draftContract);
            when(contractMapper.toResponse(draftContract)).thenReturn(new ContractResponse());

            contractService.signContract(companyId, contractId);

            assertThat(draftContract.getStatus()).isEqualTo(ContractStatus.PENDING_SIGNATURES);
            assertThat(draftContract.getSignedAt()).isNull();
        }

        @Test
        @DisplayName("DRAFT, professional attempts to sign first: throws InvalidContractOperationException")
        void signContract_professionalSignsFirst_throws() {
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));

            assertThatThrownBy(() -> contractService.signContract(professionalId, contractId))
                    .isInstanceOf(InvalidContractOperationException.class)
                    .hasMessageContaining("awaiting the company's signature");

            verify(contractRepository, never()).save(any());
        }

        @Test
        @DisplayName("PENDING_SIGNATURES, professional signs: transitions to ACTIVE and sets signedAt")
        void signContract_professionalSignsSecond_success() {
            draftContract.setStatus(ContractStatus.PENDING_SIGNATURES);
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
            when(contractRepository.save(draftContract)).thenReturn(draftContract);
            when(contractMapper.toResponse(draftContract)).thenReturn(new ContractResponse());

            contractService.signContract(professionalId, contractId);

            assertThat(draftContract.getStatus()).isEqualTo(ContractStatus.ACTIVE);
            assertThat(draftContract.getSignedAt()).isNotNull();
        }

        @Test
        @DisplayName("PENDING_SIGNATURES, company attempts to sign again: throws InvalidContractOperationException")
        void signContract_companySignsTwice_throws() {
            draftContract.setStatus(ContractStatus.PENDING_SIGNATURES);
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));

            assertThatThrownBy(() -> contractService.signContract(companyId, contractId))
                    .isInstanceOf(InvalidContractOperationException.class)
                    .hasMessageContaining("awaiting the professional's signature");
        }

        @Test
        @DisplayName("already ACTIVE: throws InvalidContractOperationException")
        void signContract_alreadyActive_throws() {
            draftContract.setStatus(ContractStatus.ACTIVE);
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));

            assertThatThrownBy(() -> contractService.signContract(companyId, contractId))
                    .isInstanceOf(InvalidContractOperationException.class)
                    .hasMessageContaining("cannot be signed");
        }

        @Test
        @DisplayName("unknown contract: throws ContractNotFoundException")
        void signContract_notFound_throws() {
            when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> contractService.signContract(companyId, contractId))
                    .isInstanceOf(ContractNotFoundException.class);
        }
    }

    // =========================================================================
    // completeContract
    // =========================================================================

    @Nested
    @DisplayName("completeContract()")
    class CompleteContract {

        private Contract activeContract;

        @BeforeEach
        void setUpContract() {
            activeContract = new Contract();
            activeContract.setId(contractId);
            activeContract.setCompanyId(companyId);
            activeContract.setProfessionalId(professionalId);
            activeContract.setStatus(ContractStatus.ACTIVE);
        }

        @Test
        @DisplayName("ACTIVE contract owned by caller: transitions to COMPLETED")
        void completeContract_success() {
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(activeContract));
            when(contractRepository.save(activeContract)).thenReturn(activeContract);
            when(contractMapper.toResponse(activeContract)).thenReturn(new ContractResponse());

            contractService.completeContract(companyId, contractId);

            assertThat(activeContract.getStatus()).isEqualTo(ContractStatus.COMPLETED);
        }

        @Test
        @DisplayName("caller is not the owning company: throws InvalidContractOperationException")
        void completeContract_notOwner_throws() {
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(activeContract));

            assertThatThrownBy(() -> contractService.completeContract(UUID.randomUUID(), contractId))
                    .isInstanceOf(InvalidContractOperationException.class)
                    .hasMessageContaining("not the owner");
        }

        @Test
        @DisplayName("contract not ACTIVE: throws InvalidContractOperationException")
        void completeContract_wrongStatus_throws() {
            activeContract.setStatus(ContractStatus.DRAFT);
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(activeContract));

            assertThatThrownBy(() -> contractService.completeContract(companyId, contractId))
                    .isInstanceOf(InvalidContractOperationException.class)
                    .hasMessageContaining("cannot be completed");
        }
    }

    // =========================================================================
    // listContractsByCompany / listContractsByProfessional
    // =========================================================================

    @Nested
    @DisplayName("listContractsByCompany() / listContractsByProfessional()")
    class Listing {

        @Test
        @DisplayName("listContractsByCompany: returns contracts mapped")
        void listContractsByCompany_returnsMapped() {
            Contract contract = new Contract();
            when(contractRepository.findByCompanyId(companyId)).thenReturn(List.of(contract));
            when(contractMapper.toResponse(contract)).thenReturn(new ContractResponse());

            List<ContractResponse> result = contractService.listContractsByCompany(companyId);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("listContractsByProfessional: returns contracts mapped")
        void listContractsByProfessional_returnsMapped() {
            Contract contract = new Contract();
            when(contractRepository.findByProfessionalId(professionalId)).thenReturn(List.of(contract));
            when(contractMapper.toResponse(contract)).thenReturn(new ContractResponse());

            List<ContractResponse> result = contractService.listContractsByProfessional(professionalId);

            assertThat(result).hasSize(1);
        }
    }
}
