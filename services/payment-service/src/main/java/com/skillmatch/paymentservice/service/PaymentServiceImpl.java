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
import com.skillmatch.paymentservice.model.enums.TransactionStatus;
import com.skillmatch.paymentservice.repository.CommissionConfigRepository;
import com.skillmatch.paymentservice.repository.InvoiceRepository;
import com.skillmatch.paymentservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final String CONTRACT_STATUS_COMPLETED = "COMPLETED";
    // Fallback used only if commission_config is somehow empty; the V1 migration
    // always seeds an initial row, so this should never be reached in practice.
    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("8.00");

    private final TransactionRepository transactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final CommissionConfigRepository commissionConfigRepository;
    private final ContractServiceClient contractServiceClient;
    private final EventPublisherService eventPublisher;
    private final PaymentMapper paymentMapper;

    // =========================================================================
    // Payment
    // =========================================================================

    @Override
    public TransactionResponse initiatePayment(UUID companyId, UUID contractId) {
        if (transactionRepository.existsByContractId(contractId)) {
            throw new InvalidPaymentOperationException(
                    "Contract with id=" + contractId + " has already been paid");
        }

        ContractSummaryResponse contract = contractServiceClient.getContract(contractId);

        if (!contract.getCompanyId().equals(companyId)) {
            throw new InvalidPaymentOperationException(
                    "Company with id=" + companyId + " is not the owner of contract id=" + contractId);
        }
        if (!CONTRACT_STATUS_COMPLETED.equals(contract.getStatus())) {
            throw new InvalidPaymentOperationException(
                    "Contract with id=" + contractId + " cannot be paid from status=" + contract.getStatus());
        }

        BigDecimal totalAmount = contract.getAmount();
        BigDecimal commissionRate = getCurrentRate();
        BigDecimal commissionAmount = totalAmount
                .multiply(commissionRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal netAmount = totalAmount.subtract(commissionAmount);

        Transaction transaction = new Transaction();
        transaction.setContractId(contractId);
        transaction.setCompanyId(companyId);
        transaction.setProfessionalId(contract.getProfessionalId());
        transaction.setTotalAmount(totalAmount);
        transaction.setCommissionAmount(commissionAmount);
        transaction.setNetAmount(netAmount);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);

        Invoice invoice = new Invoice();
        invoice.setTransaction(transaction);
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setCompanyId(companyId);
        invoice.setTotal(totalAmount);
        invoice.setCommission(commissionAmount);
        invoice.setProfessionalFee(netAmount);
        invoiceRepository.save(invoice);

        eventPublisher.publishPaymentCompleted(
                PaymentCompletedEvent.builder()
                        .data(PaymentCompletedEvent.Data.builder()
                                .transactionId(transaction.getId())
                                .contractId(contractId)
                                .projectId(contract.getProjectId())
                                .companyId(companyId)
                                .professionalId(contract.getProfessionalId())
                                .totalAmount(totalAmount)
                                .commissionAmount(commissionAmount)
                                .netAmount(netAmount)
                                .build())
                        .build());

        log.info("Payment completed: transactionId={}, contractId={}, totalAmount={}, commissionAmount={}",
                transaction.getId(), contractId, totalAmount, commissionAmount);
        return paymentMapper.toResponse(transaction);
    }

    // =========================================================================
    // Retrieval
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID transactionId) {
        return paymentMapper.toResponse(findTransactionById(transactionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> listTransactionsByCompany(UUID companyId) {
        return transactionRepository.findByCompanyId(companyId).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> listTransactionsByProfessional(UUID professionalId) {
        return transactionRepository.findByProfessionalId(professionalId).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByTransaction(UUID transactionId) {
        Invoice invoice = invoiceRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new InvoiceNotFoundException(transactionId));
        return paymentMapper.toResponse(invoice);
    }

    // =========================================================================
    // Commission configuration
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public CommissionConfigResponse getCurrentCommissionConfig() {
        return paymentMapper.toResponse(findCurrentConfig());
    }

    @Override
    public CommissionConfigResponse updateCommissionRate(UUID adminId, CommissionConfigRequest request) {
        CommissionConfig config = new CommissionConfig();
        config.setRatePercentage(request.getRatePercentage());
        config.setSetByAdminId(adminId);
        config = commissionConfigRepository.save(config);

        log.info("Commission rate updated: rate={}, adminId={}", config.getRatePercentage(), adminId);
        return paymentMapper.toResponse(config);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private BigDecimal getCurrentRate() {
        return findCurrentConfig().getRatePercentage();
    }

    private CommissionConfig findCurrentConfig() {
        return commissionConfigRepository.findFirstByOrderByEffectiveFromDesc()
                .orElseGet(() -> {
                    log.warn("No commission_config row found; falling back to default rate {}", DEFAULT_COMMISSION_RATE);
                    CommissionConfig fallback = new CommissionConfig();
                    fallback.setRatePercentage(DEFAULT_COMMISSION_RATE);
                    return fallback;
                });
    }

    private String generateInvoiceNumber() {
        return "INV-" + Year.now() + "-" + String.format("%06d", invoiceRepository.count() + 1);
    }

    private Transaction findTransactionById(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }
}
