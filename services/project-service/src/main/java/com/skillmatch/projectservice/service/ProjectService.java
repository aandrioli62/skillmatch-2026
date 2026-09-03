package com.skillmatch.projectservice.service;

import com.skillmatch.projectservice.dto.request.CandidatureRequest;
import com.skillmatch.projectservice.dto.request.ProjectCreateRequest;
import com.skillmatch.projectservice.dto.response.CandidatureResponse;
import com.skillmatch.projectservice.dto.response.ProjectResponse;

import java.util.List;
import java.util.UUID;

public interface ProjectService {

    /**
     * Creates a new project in DRAFT status, together with its requirements.
     * No event is published yet — the project is not visible to professionals until published.
     */
    ProjectResponse createProject(UUID companyId, ProjectCreateRequest request);

    /**
     * Transitions a project from DRAFT to OPEN, making it visible to professionals.
     * Publishes project.published. Only the owning company may publish its own project.
     *
     * @throws com.skillmatch.projectservice.exception.ProjectNotFoundException          if project does not exist
     * @throws com.skillmatch.projectservice.exception.InvalidProjectOperationException  if the caller is not the owner, or the project is not in DRAFT status
     */
    ProjectResponse publishProject(UUID companyId, UUID projectId);

    /**
     * Returns a single project with its requirements.
     *
     * @throws com.skillmatch.projectservice.exception.ProjectNotFoundException if project does not exist
     */
    ProjectResponse getProject(UUID projectId);

    /**
     * Returns all projects currently open for candidatures.
     */
    List<ProjectResponse> listOpenProjects();

    /**
     * Returns all projects (any status) owned by the given company.
     */
    List<ProjectResponse> listProjectsByCompany(UUID companyId);

    /**
     * Submits a candidature for an OPEN project. The professional must be VALIDATED
     * (verified synchronously against User Service) and must not have already applied.
     *
     * @throws com.skillmatch.projectservice.exception.ProjectNotFoundException          if project does not exist
     * @throws com.skillmatch.projectservice.exception.InvalidProjectOperationException  if the project is not OPEN, the caller is not a VALIDATED professional, or a candidature already exists for this pair
     * @throws com.skillmatch.projectservice.exception.UserServiceUnavailableException   if the professional's status cannot be verified
     */
    CandidatureResponse applyToProject(UUID professionalId, UUID projectId, CandidatureRequest request);

    /**
     * Returns all candidatures submitted by the given professional, across all projects.
     */
    List<CandidatureResponse> listCandidaturesByProfessional(UUID professionalId);

    /**
     * Returns all candidatures submitted for the given project. Only the owning company may list them.
     *
     * @throws com.skillmatch.projectservice.exception.ProjectNotFoundException          if project does not exist
     * @throws com.skillmatch.projectservice.exception.InvalidProjectOperationException  if the caller is not the owner
     */
    List<CandidatureResponse> listCandidaturesByProject(UUID companyId, UUID projectId);

    /**
     * Accepts a candidature. Only the owning company may accept, only one candidature per
     * project may ever be ACCEPTED, and all other PENDING candidatures for the same project
     * are automatically REJECTED. Transitions the project to ASSIGNED and publishes candidature.accepted.
     *
     * @throws com.skillmatch.projectservice.exception.ProjectNotFoundException          if project does not exist
     * @throws com.skillmatch.projectservice.exception.CandidatureNotFoundException      if candidature does not exist
     * @throws com.skillmatch.projectservice.exception.InvalidProjectOperationException  if the caller is not the owner, the candidature does not belong to the project, or the project already has an accepted candidature
     */
    CandidatureResponse acceptCandidature(UUID companyId, UUID projectId, UUID candidatureId);

    /**
     * Marks an assigned project as completed and publishes project.completed
     * (enables payment in Payment Service). Only the owning company may complete its own project.
     *
     * @throws com.skillmatch.projectservice.exception.ProjectNotFoundException          if project does not exist
     * @throws com.skillmatch.projectservice.exception.InvalidProjectOperationException  if the caller is not the owner, the project has no accepted candidature, or is not ASSIGNED/IN_PROGRESS
     */
    ProjectResponse completeProject(UUID companyId, UUID projectId);
}
