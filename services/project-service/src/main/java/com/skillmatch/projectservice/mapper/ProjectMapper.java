package com.skillmatch.projectservice.mapper;

import com.skillmatch.projectservice.dto.request.ProjectCreateRequest;
import com.skillmatch.projectservice.dto.response.ProjectResponse;
import com.skillmatch.projectservice.model.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(target = "requirements", ignore = true)
    ProjectResponse toResponse(Project project);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "companyId", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Project toEntity(ProjectCreateRequest request);
}
