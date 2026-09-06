package com.skillmatch.projectservice.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ProjectRequirementResponse {

    private UUID id;
    private String skillName;
}
