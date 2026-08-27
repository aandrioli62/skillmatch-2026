package com.skillmatch.paymentservice.repository;

import com.skillmatch.paymentservice.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByTransactionId(UUID transactionId);
}
