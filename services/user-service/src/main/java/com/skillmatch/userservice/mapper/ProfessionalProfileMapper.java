package com.skillmatch.userservice.mapper;

import com.skillmatch.userservice.dto.request.ProfessionalProfileRequest;
import com.skillmatch.userservice.dto.response.ProfessionalProfileResponse;
import com.skillmatch.userservice.model.ProfessionalProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProfessionalProfileMapper {

    @Mapping(source = "user.id", target = "userId")
    ProfessionalProfileResponse toResponse(ProfessionalProfile profile);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "reputationLevel", ignore = true)
    @Mapping(target = "avgRating", ignore = true)
    @Mapping(target = "totalReviews", ignore = true)
    ProfessionalProfile toEntity(ProfessionalProfileRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "reputationLevel", ignore = true)
    @Mapping(target = "avgRating", ignore = true)
    @Mapping(target = "totalReviews", ignore = true)
    void updateEntity(ProfessionalProfileRequest request, @MappingTarget ProfessionalProfile profile);
}
