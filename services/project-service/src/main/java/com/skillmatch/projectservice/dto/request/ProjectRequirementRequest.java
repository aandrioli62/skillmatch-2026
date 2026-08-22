package com.skillmatch.projectservice.dto.request;

import com.skillmatch.projectservice.model.enums.ReputationLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectRequirementRequest {

    @NotBlank(message = "Skill name is required")
    @Size(max = 100, message = "Skill name must not exceed 100 characters")
    private String skillName;

    private ReputationLevel minReputationLevel;
}
