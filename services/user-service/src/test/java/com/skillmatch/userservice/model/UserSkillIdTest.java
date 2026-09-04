package com.skillmatch.userservice.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserSkillIdTest {

    @Test
    void equalsAndHashCode_sameValues_areEqual() {
        UUID userId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();

        UserSkillId fromAllArgsConstructor = new UserSkillId(userId, skillId);

        UserSkillId fromNoArgsConstructor = new UserSkillId();
        fromNoArgsConstructor.setUserId(userId);
        fromNoArgsConstructor.setSkillId(skillId);

        assertThat(fromAllArgsConstructor.getUserId()).isEqualTo(userId);
        assertThat(fromAllArgsConstructor.getSkillId()).isEqualTo(skillId);
        assertThat(fromAllArgsConstructor).isEqualTo(fromNoArgsConstructor);
        assertThat(fromAllArgsConstructor.hashCode()).isEqualTo(fromNoArgsConstructor.hashCode());
        assertThat(fromAllArgsConstructor).isNotEqualTo(new UserSkillId(UUID.randomUUID(), UUID.randomUUID()));
    }
}
