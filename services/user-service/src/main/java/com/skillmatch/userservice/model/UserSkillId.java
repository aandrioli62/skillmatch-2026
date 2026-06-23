package com.skillmatch.userservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class UserSkillId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "skill_id")
    private UUID skillId;

    public UserSkillId(UUID userId, UUID skillId) {
        this.userId = userId;
        this.skillId = skillId;
    }
}
