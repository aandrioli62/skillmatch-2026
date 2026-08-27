package com.skillmatch.feedbackservice.repository;

import com.skillmatch.feedbackservice.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    List<Feedback> findByProjectId(UUID projectId);

    List<Feedback> findByReviewerId(UUID reviewerId);

    List<Feedback> findByRevieweeId(UUID revieweeId);

    Optional<Feedback> findByProjectIdAndReviewerIdAndRevieweeId(UUID projectId, UUID reviewerId, UUID revieweeId);

    boolean existsByProjectIdAndReviewerIdAndRevieweeId(UUID projectId, UUID reviewerId, UUID revieweeId);
}
