package com.skillmatch.contractservice.repository;

import com.skillmatch.contractservice.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID> {

    Optional<Contract> findByProjectId(UUID projectId);

    List<Contract> findByCompanyId(UUID companyId);

    List<Contract> findByProfessionalId(UUID professionalId);

    boolean existsByProjectId(UUID projectId);
}
