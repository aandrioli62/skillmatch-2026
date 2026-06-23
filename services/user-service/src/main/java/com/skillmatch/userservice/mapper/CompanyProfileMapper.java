package com.skillmatch.userservice.mapper;

import com.skillmatch.userservice.dto.request.CompanyProfileRequest;
import com.skillmatch.userservice.dto.response.CompanyProfileResponse;
import com.skillmatch.userservice.model.CompanyProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CompanyProfileMapper {

    @Mapping(source = "user.id", target = "userId")
    CompanyProfileResponse toResponse(CompanyProfile profile);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    CompanyProfile toEntity(CompanyProfileRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEntity(CompanyProfileRequest request, @MappingTarget CompanyProfile profile);
}
