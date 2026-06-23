package com.skillmatch.userservice.repository;

import com.skillmatch.userservice.model.CompanyProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, UUID> {

    Optional<CompanyProfile> findByUserId(UUID userId);
}
