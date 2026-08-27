package com.skillmatch.feedbackservice.service;

import com.skillmatch.feedbackservice.dto.response.FeedbackResponse;

import java.util.List;
import java.util.UUID;

public interface FeedbackService {

    /**
     * Marks a project as eligible for mutual feedback, triggered by an incoming
     * payment.completed event. Idempotent: if eligibility already exists for the
     * project (e.g. duplicate message redelivery), the call is a no-op.
     */
    void enableFeedback(UUID projectId, UUID companyId, UUID professionalId);

    /**
     * Submits feedback from the caller for the other party on a project. The reviewee
     * is derived from the project's eligibility record (company <-> professional), never
     * taken from client input. If the reviewee is the professional, recalculates their
     * aggregate rating and publishes feedback.aggregated.
     *
     * @throws com.skillmatch.feedbackservice.exception.InvalidFeedbackOperationException  if the project is not
     *         eligible for feedback yet, the caller is not a party to it, or has already reviewed the other party
     */
    FeedbackResponse submitFeedback(UUID callerId, UUID projectId, Integer rating, String comment);

    /**
     * Returns a single feedback.
     *
     * @throws com.skillmatch.feedbackservice.exception.FeedbackNotFoundException if feedback does not exist
     */
    FeedbackResponse getFeedback(UUID feedbackId);

    /**
     * Returns both feedbacks (company -> professional and professional -> company) left on a project.
     */
    List<FeedbackResponse> listByProject(UUID projectId);

    /**
     * Returns all feedback the given user has given to others.
     */
    List<FeedbackResponse> listGivenByReviewer(UUID reviewerId);

    /**
     * Returns all feedback the given user has received from others.
     */
    List<FeedbackResponse> listReceivedByReviewee(UUID revieweeId);
}
