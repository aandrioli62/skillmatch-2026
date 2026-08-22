package com.skillmatch.projectservice.exception;

import java.util.UUID;

public class CandidatureNotFoundException extends RuntimeException {

    public CandidatureNotFoundException(UUID candidatureId) {
        super("Candidature not found with id: " + candidatureId);
    }
}
