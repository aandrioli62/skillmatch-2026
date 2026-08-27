package com.skillmatch.feedbackservice.exception;

import java.util.UUID;

public class FeedbackNotFoundException extends RuntimeException {

    public FeedbackNotFoundException(UUID feedbackId) {
        super("Feedback not found with id: " + feedbackId);
    }
}
