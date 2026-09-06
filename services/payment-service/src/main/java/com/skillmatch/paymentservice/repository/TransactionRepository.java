package com.skillmatch.paymentservice.repository;

import com.skillmatch.paymentservice.model.Transaction;
import com.skillmatch.paymentservice.model.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByCompanyId(UUID companyId);

    List<Transaction> findByProfessionalId(UUID professionalId);

    Optional<Transaction> findByContractId(UUID contractId);

    boolean existsByContractId(UUID contractId);

    // Admin oversight: status and/or a creation-date range are optional (each null
    // parameter is a no-op filter), so the same query serves "all transactions",
    // "just COMPLETED ones", "just this month", or any combination of the two.
    @Query("SELECT t FROM Transaction t WHERE "
            + "(:status IS NULL OR t.status = :status) AND "
            + "(:from IS NULL OR t.createdAt >= :from) AND "
            + "(:to IS NULL OR t.createdAt <= :to)")
    Page<Transaction> findWithFilters(
            @Param("status") TransactionStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT "
            + "COALESCE(SUM(t.totalAmount), 0) AS totalVolume, "
            + "COALESCE(SUM(t.commissionAmount), 0) AS totalCommission, "
            + "COALESCE(SUM(t.netAmount), 0) AS totalNet, "
            + "COUNT(t) AS count "
            + "FROM Transaction t WHERE "
            + "(:status IS NULL OR t.status = :status) AND "
            + "(:from IS NULL OR t.createdAt >= :from) AND "
            + "(:to IS NULL OR t.createdAt <= :to)")
    TransactionSummaryProjection summarize(
            @Param("status") TransactionStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
