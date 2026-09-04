package com.skillmatch.userservice.mapper;

import com.skillmatch.userservice.dto.response.SkillResponse;
import com.skillmatch.userservice.model.Skill;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SkillMapperTest {

    private final SkillMapper skillMapper = Mappers.getMapper(SkillMapper.class);

    @Test
    void toResponse_mapsAllFields() {
        Skill skill = new Skill();
        skill.setId(UUID.randomUUID());
        skill.setName("Java");
        skill.setCategory("Backend");

        SkillResponse response = skillMapper.toResponse(skill);

        assertThat(response.getId()).isEqualTo(skill.getId());
        assertThat(response.getName()).isEqualTo("Java");
        assertThat(response.getCategory()).isEqualTo("Backend");
    }
}
