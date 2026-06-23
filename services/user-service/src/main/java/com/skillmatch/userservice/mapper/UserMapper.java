package com.skillmatch.userservice.mapper;

import com.skillmatch.userservice.dto.request.UserRegistrationRequest;
import com.skillmatch.userservice.dto.response.UserResponse;
import com.skillmatch.userservice.model.User;
import com.skillmatch.userservice.model.enums.UserStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(UserRegistrationRequest request);
}
