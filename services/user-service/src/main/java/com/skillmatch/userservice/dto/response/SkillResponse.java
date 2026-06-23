package com.skillmatch.userservice.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SkillResponse {

    private UUID id;
    private String name;
    private String category;
}
