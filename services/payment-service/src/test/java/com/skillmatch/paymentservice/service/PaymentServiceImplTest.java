package com.skillmatch.paymentservice.service;

import com.skillmatch.paymentservice.client.ContractServiceClient;
import com.skillmatch.paymentservice.client.ContractSummaryResponse;
import com.skillmatch.paymentservice.dto.request.CommissionConfigRequest;
import com.skillmatch.paymentservice.dto.response.CommissionConfigResponse;
import com.skillmatch.paymentservice.dto.response.InvoiceResponse;
import com.skillmatch.paymentservice.dto.response.TransactionResponse;
import com.skillmatch.paymentservice.event.PaymentCompletedEvent;
import com.skillmatch.paymentservice.exception.InvalidPaymentOperationException;
import com.skillmatch.paymentservice.exception.InvoiceNotFoundException;
import com.skillmatch.paymentservice.exception.TransactionNotFoundException;
import com.skillmatch.paymentservice.mapper.PaymentMapper;
import com.skillmatch.paymentservice.model.CommissionConfig;
import com.skillmatch.paymentservice.model.Invoice;
import com.skillmatch.paymentservice.model.Transaction;
import com.skillmatch.paymentservice.repository.CommissionConfigRepository;
import com.skillmatch.paymentservice.repository.InvoiceRepository;
import com.skillmatch.paymentservice.repository.TransactionRepository;
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
@DisplayName("PaymentServiceImpl — Unit Tests")
class PaymentServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private CommissionConfigRepository commissionConfigRepository;
    @Mock
    private ContractServiceClient contractServiceClient;
    @Mock
    private EventPublisherService eventPublisher;
    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID companyId;
    private UUID professionalId;
    private UUID contractId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        professionalId = UUID.randomUUID();
        contractId = UUID.randomUUID();
    }

    private ContractSummaryResponse completedContract(BigDecimal amount) {
        ContractSummaryResponse contract = new ContractSummaryResponse();
        contract.setId(contractId);
        contract.setCompanyId(companyId);
        contract.setProfessionalId(professionalId);
        contract.setAmount(amount);
        contract.setStatus("COMPLETED");
        return contract;
    }

    private CommissionConfig configWithRate(String rate) {
        CommissionConfig config = new CommissionConfig();
        config.setRatePercentage(new BigDecimal(rate));
        return config;
    }

    // =========================================================================
    // initiatePayment — commission calculation
    // =========================================================================

    @Nested
    @DisplayName("initiatePayment()")
    class InitiatePayment {

        @Test
        @DisplayName("8% rate on 1000.00: commission=80.00, net=920.00")
        void initiatePayment_calculatesCommissionAtDefaultRate() {
            when(transactionRepository.existsByContractId(contractId)).thenReturn(false);
            when(contractServiceClient.getContract(contractId)).thenReturn(completedContract(BigDecimal.valueOf(1000)));
            when(commissionConfigRepository.findFirstByOrderByEffectiveFromDesc())
                    .thenReturn(Optional.of(configWithRate("8.00")));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
            when(invoiceRepository.count()).thenReturn(0L);
            when(paymentMapper.toResponse(any(Transaction.class))).thenReturn(new TransactionResponse());

            paymentService.initiatePayment(companyId, contractId);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            Transaction saved = captor.getValue();
            assertThat(saved.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
            assertThat(saved.getCommissionAmount()).isEqualByComparingTo(new BigDecimal("80.00"));
            assertThat(saved.getNetAmount()).isEqualByComparingTo(new BigDecimal("920.00"));

            ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
            verify(invoiceRepository).save(invoiceCaptor.capture());
            assertThat(invoiceCaptor.getValue().getCommission()).isEqualByComparingTo(new BigDecimal("80.00"));
            assertThat(invoiceCaptor.getValue().getProfessionalFee()).isEqualByComparingTo(new BigDecimal("920.00"));

            ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
            verify(eventPublisher).publishPaymentCompleted(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getData().getCommissionAmount()).isEqualByComparingTo(new BigDecimal("80.00"));
        }

        @Test
        @DisplayName("rounds commission to 2 decimal places (HALF_UP)")
        void initiatePayment_roundsCommission() {
            // 333.33 * 8% = 26.6664 -> rounds to 26.67
            when(transactionRepository.existsByContractId(contractId)).thenReturn(false);
            when(contractServiceClient.getContract(contractId)).thenReturn(completedContract(new BigDecimal("333.33")));
            when(commissionConfigRepository.findFirstByOrderByEffectiveFromDesc())
                    .thenReturn(Optional.of(configWithRate("8.00")));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
            when(invoiceRepository.count()).thenReturn(0L);
            when(paymentMapper.toResponse(any(Transaction.class))).thenReturn(new TransactionResponse());

            paymentService.initiatePayment(companyId, contractId);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            assertThat(captor.getValue().getCommissionAmount()).isEqualByComparingTo(new BigDecimal("26.67"));
            assertThat(captor.getValue().getNetAmount()).isEqualByComparingTo(new BigDecimal("306.66"));
        }

        @Test
        @DisplayName("uses the most recently configured rate, not the default")
        void initiatePayment_usesLatestConfiguredRate() {
            when(transactionRepository.existsByContractId(contractId)).thenReturn(false);
            when(contractServiceClient.getContract(contractId)).thenReturn(completedContract(BigDecimal.valueOf(1000)));
            when(commissionConfigRepository.findFirstByOrderByEffectiveFromDesc())
                    .thenReturn(Optional.of(configWithRate("10.00")));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
            when(invoiceRepository.count()).thenReturn(0L);
            when(paymentMapper.toResponse(any(Transaction.class))).thenReturn(new TransactionResponse());

            paymentService.initiatePayment(companyId, contractId);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            assertThat(captor.getValue().getCommissionAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("already paid: throws InvalidPaymentOperationException without calling Contract Service")
        void initiatePayment_alreadyPaid_throws() {
            when(transactionRepository.existsByContractId(contractId)).thenReturn(true);

            assertThatThrownBy(() -> paymentService.initiatePayment(companyId, contractId))
                    .isInstanceOf(InvalidPaymentOperationException.class)
                    .hasMessageContaining("already been paid");

            verifyNoMoreCalls();
        }

        @Test
        @DisplayName("caller is not the contract's company: throws InvalidPaymentOperationException")
        void initiatePayment_notOwner_throws() {
            when(transactionRepository.existsByContractId(contractId)).thenReturn(false);
            when(contractServiceClient.getContract(contractId)).thenReturn(completedContract(BigDecimal.valueOf(1000)));

            assertThatThrownBy(() -> paymentService.initiatePayment(UUID.randomUUID(), contractId))
                    .isInstanceOf(InvalidPaymentOperationException.class)
                    .hasMessageContaining("not the owner");

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("contract not COMPLETED: throws InvalidPaymentOperationException")
        void initiatePayment_contractNotCompleted_throws() {
            ContractSummaryResponse activeContract = completedContract(BigDecimal.valueOf(1000));
            activeContract.setStatus("ACTIVE");

            when(transactionRepository.existsByContractId(contractId)).thenReturn(false);
            when(contractServiceClient.getContract(contractId)).thenReturn(activeContract);

            assertThatThrownBy(() -> paymentService.initiatePayment(companyId, contractId))
                    .isInstanceOf(InvalidPaymentOperationException.class)
                    .hasMessageContaining("cannot be paid");

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("no commission_config row exists: falls back to the 8.00 default")
        void initiatePayment_noConfigRow_fallsBackToDefault() {
            when(transactionRepository.existsByContractId(contractId)).thenReturn(false);
            when(contractServiceClient.getContract(contractId)).thenReturn(completedContract(BigDecimal.valueOf(1000)));
            when(commissionConfigRepository.findFirstByOrderByEffectiveFromDesc()).thenReturn(Optional.empty());
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
            when(invoiceRepository.count()).thenReturn(0L);
            when(paymentMapper.toResponse(any(Transaction.class))).thenReturn(new TransactionResponse());

            paymentService.initiatePayment(companyId, contractId);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            assertThat(captor.getValue().getCommissionAmount()).isEqualByComparingTo(new BigDecimal("80.00"));
        }

        private void verifyNoMoreCalls() {
            verify(contractServiceClient, never()).getContract(any());
            verify(transactionRepository, never()).save(any());
        }
    }

    // =========================================================================
    // Retrieval
    // =========================================================================

    @Nested
    @DisplayName("getTransaction() / listing")
    class Retrieval {

        @Test
        @DisplayName("existing transaction: returns mapped response")
        void getTransaction_found() {
            UUID transactionId = UUID.randomUUID();
            Transaction transaction = new Transaction();
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
            when(paymentMapper.toResponse(transaction)).thenReturn(new TransactionResponse());

            TransactionResponse result = paymentService.getTransaction(transactionId);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("unknown transaction: throws TransactionNotFoundException")
        void getTransaction_notFound() {
            UUID transactionId = UUID.randomUUID();
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getTransaction(transactionId))
                    .isInstanceOf(TransactionNotFoundException.class);
        }

        @Test
        @DisplayName("listTransactionsByCompany: returns mapped list")
        void listTransactionsByCompany_returnsMapped() {
            Transaction transaction = new Transaction();
            when(transactionRepository.findByCompanyId(companyId)).thenReturn(List.of(transaction));
            when(paymentMapper.toResponse(transaction)).thenReturn(new TransactionResponse());

            List<TransactionResponse> result = paymentService.listTransactionsByCompany(companyId);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("getInvoiceByTransaction: unknown transaction throws InvoiceNotFoundException")
        void getInvoiceByTransaction_notFound() {
            UUID transactionId = UUID.randomUUID();
            when(invoiceRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getInvoiceByTransaction(transactionId))
                    .isInstanceOf(InvoiceNotFoundException.class);
        }
    }

    // =========================================================================
    // Commission configuration
    // =========================================================================

    @Nested
    @DisplayName("commission configuration")
    class CommissionConfiguration {

        @Test
        @DisplayName("updateCommissionRate: saves a new row with the admin id, does not mutate the old one")
        void updateCommissionRate_savesNewRow() {
            UUID adminId = UUID.randomUUID();
            CommissionConfigRequest request = new CommissionConfigRequest();
            request.setRatePercentage(new BigDecimal("10.00"));

            when(commissionConfigRepository.save(any(CommissionConfig.class))).thenAnswer(inv -> inv.getArgument(0));
            when(paymentMapper.toResponse(any(CommissionConfig.class))).thenReturn(new CommissionConfigResponse());

            paymentService.updateCommissionRate(adminId, request);

            ArgumentCaptor<CommissionConfig> captor = ArgumentCaptor.forClass(CommissionConfig.class);
            verify(commissionConfigRepository).save(captor.capture());
            assertThat(captor.getValue().getRatePercentage()).isEqualByComparingTo(new BigDecimal("10.00"));
            assertThat(captor.getValue().getSetByAdminId()).isEqualTo(adminId);
        }
    }
}
