package com.skillmatch.userservice.mapper;

import com.skillmatch.userservice.dto.response.ProfessionalSkillResponse;
import com.skillmatch.userservice.dto.response.SkillResponse;
import com.skillmatch.userservice.model.Skill;
import com.skillmatch.userservice.model.UserSkill;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SkillMapper {

    SkillResponse toResponse(Skill skill);

    @Mapping(source = "skill.id", target = "skillId")
    @Mapping(source = "skill.name", target = "skillName")
    @Mapping(source = "skill.category", target = "category")
    ProfessionalSkillResponse toProfessionalSkillResponse(UserSkill userSkill);
}
