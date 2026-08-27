package com.skillmatch.paymentservice.repository;

import com.skillmatch.paymentservice.model.CommissionConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CommissionConfigRepository extends JpaRepository<CommissionConfig, UUID> {

    Optional<CommissionConfig> findFirstByOrderByEffectiveFromDesc();
}
