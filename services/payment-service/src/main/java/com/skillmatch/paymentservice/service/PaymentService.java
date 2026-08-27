package com.skillmatch.paymentservice.service;

import com.skillmatch.paymentservice.dto.request.CommissionConfigRequest;
import com.skillmatch.paymentservice.dto.response.CommissionConfigResponse;
import com.skillmatch.paymentservice.dto.response.InvoiceResponse;
import com.skillmatch.paymentservice.dto.response.TransactionResponse;

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
     * Returns a single transaction.
     *
     * @throws com.skillmatch.paymentservice.exception.TransactionNotFoundException if transaction does not exist
     */
    TransactionResponse getTransaction(UUID transactionId);

    /**
     * Returns all transactions where the given user is the paying company.
     */
    List<TransactionResponse> listTransactionsByCompany(UUID companyId);

    /**
     * Returns all transactions where the given user is the paid professional.
     */
    List<TransactionResponse> listTransactionsByProfessional(UUID professionalId);

    /**
     * Returns the invoice generated for a transaction.
     *
     * @throws com.skillmatch.paymentservice.exception.InvoiceNotFoundException if no invoice exists for that transaction
     */
    InvoiceResponse getInvoiceByTransaction(UUID transactionId);

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
