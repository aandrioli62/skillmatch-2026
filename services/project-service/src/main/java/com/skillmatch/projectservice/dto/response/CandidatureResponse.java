package com.skillmatch.projectservice.dto.response;

import com.skillmatch.projectservice.model.enums.CandidatureStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class CandidatureResponse {

    private UUID id;
    private UUID projectId;
    private UUID professionalId;
    private CandidatureStatus status;
    private String coverLetter;
    private LocalDateTime appliedAt;
}
