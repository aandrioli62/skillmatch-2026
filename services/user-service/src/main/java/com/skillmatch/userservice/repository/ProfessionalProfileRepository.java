package com.skillmatch.userservice.repository;

import com.skillmatch.userservice.model.ProfessionalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionalProfileRepository extends JpaRepository<ProfessionalProfile, UUID> {

    Optional<ProfessionalProfile> findByUserId(UUID userId);

    /**
     * Returns all professional profiles whose associated user is VALIDATED and
     * whose skill set contains a skill with the given name (case-insensitive).
     */
    @Query("""
            SELECT pp FROM ProfessionalProfile pp
            WHERE pp.user.status = com.skillmatch.userservice.model.enums.UserStatus.VALIDATED
              AND EXISTS (
                  SELECT us FROM UserSkill us
                  WHERE us.user = pp.user
                    AND LOWER(us.skill.name) = LOWER(:skillName)
              )
            """)
    List<ProfessionalProfile> findValidatedBySkillName(@Param("skillName") String skillName);
}
