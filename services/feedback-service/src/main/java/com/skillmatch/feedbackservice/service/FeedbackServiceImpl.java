package com.skillmatch.feedbackservice.service;

import com.skillmatch.feedbackservice.dto.response.FeedbackResponse;
import com.skillmatch.feedbackservice.event.FeedbackAggregatedEvent;
import com.skillmatch.feedbackservice.exception.FeedbackNotFoundException;
import com.skillmatch.feedbackservice.exception.InvalidFeedbackOperationException;
import com.skillmatch.feedbackservice.mapper.FeedbackMapper;
import com.skillmatch.feedbackservice.model.Feedback;
import com.skillmatch.feedbackservice.model.FeedbackEligibility;
import com.skillmatch.feedbackservice.repository.FeedbackEligibilityRepository;
import com.skillmatch.feedbackservice.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackEligibilityRepository eligibilityRepository;
    private final EventPublisherService eventPublisher;
    private final FeedbackMapper feedbackMapper;

    // =========================================================================
    // Eligibility (event-driven)
    // =========================================================================

    @Override
    public void enableFeedback(UUID projectId, UUID companyId, UUID professionalId) {
        if (eligibilityRepository.existsByProjectId(projectId)) {
            log.warn("Feedback already enabled for projectId={}, skipping duplicate payment.completed event", projectId);
            return;
        }

        FeedbackEligibility eligibility = new FeedbackEligibility();
        eligibility.setProjectId(projectId);
        eligibility.setCompanyId(companyId);
        eligibility.setProfessionalId(professionalId);
        eligibilityRepository.save(eligibility);

        log.info("Feedback enabled: projectId={}, companyId={}, professionalId={}", projectId, companyId, professionalId);
    }

    // =========================================================================
    // Submission
    // =========================================================================

    @Override
    public FeedbackResponse submitFeedback(UUID callerId, UUID projectId, Integer rating, String comment) {
        FeedbackEligibility eligibility = eligibilityRepository.findByProjectId(projectId)
                .orElseThrow(() -> new InvalidFeedbackOperationException(
                        "Project with id=" + projectId + " is not yet eligible for feedback"));

        UUID revieweeId;
        if (callerId.equals(eligibility.getCompanyId())) {
            revieweeId = eligibility.getProfessionalId();
        } else if (callerId.equals(eligibility.getProfessionalId())) {
            revieweeId = eligibility.getCompanyId();
        } else {
            throw new InvalidFeedbackOperationException(
                    "Caller with id=" + callerId + " is not a party to project id=" + projectId);
        }

        if (feedbackRepository.existsByProjectIdAndReviewerIdAndRevieweeId(projectId, callerId, revieweeId)) {
            throw new InvalidFeedbackOperationException(
                    "Caller with id=" + callerId + " has already reviewed this party for project id=" + projectId);
        }

        Feedback feedback = new Feedback();
        feedback.setProjectId(projectId);
        feedback.setReviewerId(callerId);
        feedback.setRevieweeId(revieweeId);
        feedback.setRating(rating);
        feedback.setComment(comment);
        feedback = feedbackRepository.save(feedback);

        log.info("Feedback submitted: feedbackId={}, projectId={}, reviewerId={}, revieweeId={}, rating={}",
                feedback.getId(), projectId, callerId, revieweeId, rating);

        // Only professionals accrue a reputation (CLAUDE.md #4); a professional reviewing
        // a company has nothing to aggregate.
        if (revieweeId.equals(eligibility.getProfessionalId())) {
            recalculateAndPublishReputation(eligibility.getProfessionalId());
        }

        return feedbackMapper.toResponse(feedback);
    }

    // =========================================================================
    // Retrieval
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponse getFeedback(UUID feedbackId) {
        return feedbackMapper.toResponse(findFeedbackById(feedbackId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> listByProject(UUID projectId) {
        return feedbackRepository.findByProjectId(projectId).stream()
                .map(feedbackMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> listGivenByReviewer(UUID reviewerId) {
        return feedbackRepository.findByReviewerId(reviewerId).stream()
                .map(feedbackMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> listReceivedByReviewee(UUID revieweeId) {
        return feedbackRepository.findByRevieweeId(revieweeId).stream()
                .map(feedbackMapper::toResponse)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void recalculateAndPublishReputation(UUID professionalId) {
        List<Feedback> received = feedbackRepository.findByRevieweeId(professionalId);
        int totalReviews = received.size();
        BigDecimal avgRating = received.stream()
                .map(f -> BigDecimal.valueOf(f.getRating()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(totalReviews), 2, RoundingMode.HALF_UP);

        eventPublisher.publishFeedbackAggregated(
                FeedbackAggregatedEvent.builder()
                        .data(FeedbackAggregatedEvent.Data.builder()
                                .professionalId(professionalId)
                                .avgRating(avgRating)
                                .totalReviews(totalReviews)
                                .build())
                        .build());
    }

    private Feedback findFeedbackById(UUID feedbackId) {
        return feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new FeedbackNotFoundException(feedbackId));
    }
}
