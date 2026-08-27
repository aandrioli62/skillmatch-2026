package com.skillmatch.paymentservice.repository;

import com.skillmatch.paymentservice.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByCompanyId(UUID companyId);

    List<Transaction> findByProfessionalId(UUID professionalId);

    Optional<Transaction> findByContractId(UUID contractId);

    boolean existsByContractId(UUID contractId);
}
