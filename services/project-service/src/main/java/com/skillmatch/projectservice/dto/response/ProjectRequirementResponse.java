package com.skillmatch.projectservice.dto.response;

import com.skillmatch.projectservice.model.enums.ReputationLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ProjectRequirementResponse {

    private UUID id;
    private String skillName;
    private ReputationLevel minReputationLevel;
}
