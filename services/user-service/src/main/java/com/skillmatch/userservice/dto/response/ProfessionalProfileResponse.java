package com.skillmatch.userservice.dto.response;

import com.skillmatch.userservice.model.enums.ReputationLevel;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ProfessionalProfileResponse {

    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String bio;
    private ReputationLevel reputationLevel;
    private BigDecimal avgRating;
    private Integer totalReviews;
}
