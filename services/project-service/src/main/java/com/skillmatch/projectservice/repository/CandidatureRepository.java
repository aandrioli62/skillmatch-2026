package com.skillmatch.projectservice.repository;

import com.skillmatch.projectservice.model.Candidature;
import com.skillmatch.projectservice.model.enums.CandidatureStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidatureRepository extends JpaRepository<Candidature, UUID> {

    List<Candidature> findByProjectId(UUID projectId);

    List<Candidature> findByProfessionalId(UUID professionalId);

    Optional<Candidature> findByProjectIdAndProfessionalId(UUID projectId, UUID professionalId);

    boolean existsByProjectIdAndStatus(UUID projectId, CandidatureStatus status);

    Optional<Candidature> findFirstByProjectIdAndStatus(UUID projectId, CandidatureStatus status);
}
