package com.skillmatch.feedbackservice.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class FeedbackResponse {

    private UUID id;
    private UUID projectId;
    private UUID reviewerId;
    private UUID revieweeId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
