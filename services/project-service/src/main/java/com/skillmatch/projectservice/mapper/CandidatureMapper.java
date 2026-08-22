package com.skillmatch.projectservice.mapper;

import com.skillmatch.projectservice.dto.request.CandidatureRequest;
import com.skillmatch.projectservice.dto.response.CandidatureResponse;
import com.skillmatch.projectservice.model.Candidature;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CandidatureMapper {

    @Mapping(source = "project.id", target = "projectId")
    CandidatureResponse toResponse(Candidature candidature);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "professionalId", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "appliedAt", ignore = true)
    Candidature toEntity(CandidatureRequest request);
}
