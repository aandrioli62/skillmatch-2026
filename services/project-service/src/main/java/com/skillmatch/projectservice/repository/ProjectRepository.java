package com.skillmatch.projectservice.repository;

import com.skillmatch.projectservice.model.Project;
import com.skillmatch.projectservice.model.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByCompanyId(UUID companyId);

    List<Project> findByStatus(ProjectStatus status);
}
