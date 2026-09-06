package com.skillmatch.paymentservice.service;

import com.skillmatch.paymentservice.dto.request.CommissionConfigRequest;
import com.skillmatch.paymentservice.dto.response.CommissionConfigResponse;
import com.skillmatch.paymentservice.dto.response.InvoiceResponse;
import com.skillmatch.paymentservice.dto.response.TransactionResponse;
import com.skillmatch.paymentservice.dto.response.TransactionSummaryResponse;
import com.skillmatch.paymentservice.model.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PaymentService {

    /**
     * Initiates payment for a COMPLETED contract: fetches the contract from Contract
     * Service, computes the platform commission at the currently active rate, records
     * the transaction (mocked as instantly successful — no external payment gateway is
     * integrated), generates the company's invoice, and publishes payment.completed.
     *
     * @throws com.skillmatch.paymentservice.exception.InvalidPaymentOperationException     if the caller is not the
     *         contract's company, the contract is not COMPLETED, or it has already been paid
     * @throws com.skillmatch.paymentservice.exception.ContractServiceUnavailableException  if the contract cannot be verified
     */
    TransactionResponse initiatePayment(UUID companyId, UUID contractId);

    /**
     * Returns a single transaction. Only a party to the transaction (its company or its
     * professional) or an admin may view it.
     *
     * @throws com.skillmatch.paymentservice.exception.TransactionNotFoundException     if transaction does not exist
     * @throws org.springframework.security.access.AccessDeniedException               if the caller is not a party and not an admin
     */
    TransactionResponse getTransaction(UUID transactionId, UUID callerId, boolean isAdmin);

    /**
     * Returns all transactions where the given user is the paying company.
     */
    List<TransactionResponse> listTransactionsByCompany(UUID companyId);

    /**
     * Returns all transactions where the given user is the paid professional.
     */
    List<TransactionResponse> listTransactionsByProfessional(UUID professionalId);

    /**
     * Admin: returns a paginated list of all transactions platform-wide, ordered by
     * creation date descending. Each filter is optional (null = no restriction on it).
     */
    Page<TransactionResponse> listAllTransactions(
            TransactionStatus status, LocalDateTime from, LocalDateTime to, Pageable pageable);

    /**
     * Admin: aggregate totals (volume, commission, net, count) for the same optional
     * filters as {@link #listAllTransactions} — the "earnings" half of transaction oversight.
     */
    TransactionSummaryResponse getTransactionSummary(TransactionStatus status, LocalDateTime from, LocalDateTime to);

    /**
     * Returns the invoice generated for a transaction. Only a party to the underlying
     * transaction (its company or its professional) or an admin may view it.
     *
     * @throws com.skillmatch.paymentservice.exception.InvoiceNotFoundException  if no invoice exists for that transaction
     * @throws org.springframework.security.access.AccessDeniedException        if the caller is not a party and not an admin
     */
    InvoiceResponse getInvoiceByTransaction(UUID transactionId, UUID callerId, boolean isAdmin);

    /**
     * Returns the currently active commission rate.
     */
    CommissionConfigResponse getCurrentCommissionConfig();

    /**
     * Records a new commission rate, effective immediately. Past transactions are
     * unaffected — only future payments use the new rate.
     */
    CommissionConfigResponse updateCommissionRate(UUID adminId, CommissionConfigRequest request);
}
