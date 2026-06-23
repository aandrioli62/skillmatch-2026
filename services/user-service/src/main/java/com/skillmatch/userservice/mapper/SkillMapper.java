package com.skillmatch.userservice.mapper;

import com.skillmatch.userservice.dto.response.SkillResponse;
import com.skillmatch.userservice.model.Skill;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SkillMapper {

    SkillResponse toResponse(Skill skill);
}
