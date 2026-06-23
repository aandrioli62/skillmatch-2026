package com.skillmatch.userservice.repository;

import com.skillmatch.userservice.model.UserSkill;
import com.skillmatch.userservice.model.UserSkillId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserSkillRepository extends JpaRepository<UserSkill, UserSkillId> {

    List<UserSkill> findByIdUserId(UUID userId);
}
