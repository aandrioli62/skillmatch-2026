package com.skillmatch.projectservice.dto.response;

import com.skillmatch.projectservice.model.enums.ProjectStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ProjectResponse {

    private UUID id;
    private UUID companyId;
    private String title;
    private String description;
    private Integer durationDays;
    private BigDecimal budget;
    private ProjectStatus status;
    private List<ProjectRequirementResponse> requirements;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
