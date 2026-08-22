package com.skillmatch.projectservice.repository;

import com.skillmatch.projectservice.model.ProjectRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectRequirementRepository extends JpaRepository<ProjectRequirement, UUID> {

    List<ProjectRequirement> findByProjectId(UUID projectId);
}
