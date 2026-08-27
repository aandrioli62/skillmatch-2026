package com.skillmatch.feedbackservice.repository;

import com.skillmatch.feedbackservice.model.FeedbackEligibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FeedbackEligibilityRepository extends JpaRepository<FeedbackEligibility, UUID> {

    Optional<FeedbackEligibility> findByProjectId(UUID projectId);

    boolean existsByProjectId(UUID projectId);
}
