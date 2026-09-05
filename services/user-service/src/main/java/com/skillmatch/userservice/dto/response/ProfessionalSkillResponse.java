package com.skillmatch.userservice.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ProfessionalSkillResponse {

    private UUID skillId;
    private String skillName;
    private String category;
    private String certificationUrl;
}
