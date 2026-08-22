package com.skillmatch.projectservice.mapper;

import com.skillmatch.projectservice.dto.request.ProjectRequirementRequest;
import com.skillmatch.projectservice.dto.response.ProjectRequirementResponse;
import com.skillmatch.projectservice.model.ProjectRequirement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectRequirementMapper {

    ProjectRequirementResponse toResponse(ProjectRequirement requirement);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    ProjectRequirement toEntity(ProjectRequirementRequest request);
}
