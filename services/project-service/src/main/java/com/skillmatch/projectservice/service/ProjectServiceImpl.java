package com.skillmatch.projectservice.service;

import com.skillmatch.projectservice.client.UserServiceClient;
import com.skillmatch.projectservice.client.UserStatusResponse;
import com.skillmatch.projectservice.dto.request.CandidatureRequest;
import com.skillmatch.projectservice.dto.request.ProjectCreateRequest;
import com.skillmatch.projectservice.dto.response.CandidatureResponse;
import com.skillmatch.projectservice.dto.response.ProjectResponse;
import com.skillmatch.projectservice.event.CandidatureAcceptedEvent;
import com.skillmatch.projectservice.event.ProjectCompletedEvent;
import com.skillmatch.projectservice.event.ProjectPublishedEvent;
import com.skillmatch.projectservice.exception.CandidatureNotFoundException;
import com.skillmatch.projectservice.exception.InvalidProjectOperationException;
import com.skillmatch.projectservice.exception.ProjectNotFoundException;
import com.skillmatch.projectservice.mapper.CandidatureMapper;
import com.skillmatch.projectservice.mapper.ProjectMapper;
import com.skillmatch.projectservice.mapper.ProjectRequirementMapper;
import com.skillmatch.projectservice.model.Candidature;
import com.skillmatch.projectservice.model.Project;
import com.skillmatch.projectservice.model.ProjectRequirement;
import com.skillmatch.projectservice.model.enums.CandidatureStatus;
import com.skillmatch.projectservice.model.enums.ProjectStatus;
import com.skillmatch.projectservice.repository.CandidatureRepository;
import com.skillmatch.projectservice.repository.ProjectRepository;
import com.skillmatch.projectservice.repository.ProjectRequirementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private static final String ROLE_PROFESSIONAL = "PROFESSIONAL";
    private static final String STATUS_VALIDATED = "VALIDATED";

    private final ProjectRepository projectRepository;
    private final ProjectRequirementRepository requirementRepository;
    private final CandidatureRepository candidatureRepository;
    private final UserServiceClient userServiceClient;
    private final EventPublisherService eventPublisher;
    private final ProjectMapper projectMapper;
    private final ProjectRequirementMapper requirementMapper;
    private final CandidatureMapper candidatureMapper;

    // =========================================================================
    // Creation & publication
    // =========================================================================

    @Override
    public ProjectResponse createProject(UUID companyId, ProjectCreateRequest request) {
        Project project = projectMapper.toEntity(request);
        project.setCompanyId(companyId);
        project = projectRepository.save(project);

        Project savedProject = project;
        List<ProjectRequirement> requirements = request.getRequirements().stream()
                .map(reqDto -> {
                    ProjectRequirement requirement = requirementMapper.toEntity(reqDto);
                    requirement.setProject(savedProject);
                    return requirementRepository.save(requirement);
                })
                .collect(Collectors.toList());

        log.info("Project created: projectId={}, companyId={}", project.getId(), companyId);
        return toResponseWithRequirements(project, requirements);
    }

    @Override
    public ProjectResponse publishProject(UUID companyId, UUID projectId) {
        Project project = findProjectById(projectId);
        assertOwnership(project, companyId);

        if (project.getStatus() != ProjectStatus.DRAFT) {
            throw new InvalidProjectOperationException(
                    "Project with id=" + projectId + " cannot be published from status=" + project.getStatus());
        }

        project.setStatus(ProjectStatus.OPEN);
        project = projectRepository.save(project);

        List<ProjectRequirement> requirements = requirementRepository.findByProjectId(projectId);

        eventPublisher.publishProjectPublished(
                ProjectPublishedEvent.builder()
                        .data(ProjectPublishedEvent.Data.builder()
                                .projectId(project.getId())
                                .companyId(project.getCompanyId())
                                .title(project.getTitle())
                                .requiredSkills(requirements.stream()
                                        .map(ProjectRequirement::getSkillName)
                                        .collect(Collectors.toList()))
                                .build())
                        .build());

        log.info("Project published: projectId={}", projectId);
        return toResponseWithRequirements(project, requirements);
    }

    // =========================================================================
    // Retrieval
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId) {
        Project project = findProjectById(projectId);
        return toResponseWithRequirements(project, requirementRepository.findByProjectId(projectId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> listOpenProjects() {
        return projectRepository.findByStatus(ProjectStatus.OPEN).stream()
                .map(project -> toResponseWithRequirements(project, requirementRepository.findByProjectId(project.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjectsByCompany(UUID companyId) {
        return projectRepository.findByCompanyId(companyId).stream()
                .map(project -> toResponseWithRequirements(project, requirementRepository.findByProjectId(project.getId())))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Candidature
    // =========================================================================

    @Override
    public CandidatureResponse applyToProject(UUID professionalId, UUID projectId, CandidatureRequest request) {
        Project project = findProjectById(projectId);

        if (project.getStatus() != ProjectStatus.OPEN) {
            throw new InvalidProjectOperationException(
                    "Project with id=" + projectId + " is not open for candidatures (status=" + project.getStatus() + ")");
        }

        UserStatusResponse professional = userServiceClient.getUserStatus(professionalId);
        if (!ROLE_PROFESSIONAL.equals(professional.getRole())) {
            throw new InvalidProjectOperationException("User with id=" + professionalId + " is not a PROFESSIONAL");
        }
        if (!STATUS_VALIDATED.equals(professional.getStatus())) {
            throw new InvalidProjectOperationException(
                    "Professional with id=" + professionalId
                            + " must be VALIDATED before applying to projects (current status=" + professional.getStatus() + ")");
        }

        if (candidatureRepository.findByProjectIdAndProfessionalId(projectId, professionalId).isPresent()) {
            throw new InvalidProjectOperationException(
                    "Professional with id=" + professionalId + " has already applied to project id=" + projectId);
        }

        Candidature candidature = candidatureMapper.toEntity(request);
        candidature.setProject(project);
        candidature.setProfessionalId(professionalId);
        candidature = candidatureRepository.save(candidature);

        log.info("Candidature submitted: candidatureId={}, projectId={}, professionalId={}",
                candidature.getId(), projectId, professionalId);
        return candidatureMapper.toResponse(candidature);
    }

    @Override
    public CandidatureResponse acceptCandidature(UUID companyId, UUID projectId, UUID candidatureId) {
        Project project = findProjectById(projectId);
        assertOwnership(project, companyId);

        if (candidatureRepository.existsByProjectIdAndStatus(projectId, CandidatureStatus.ACCEPTED)) {
            throw new InvalidProjectOperationException(
                    "Project with id=" + projectId + " already has an accepted candidature");
        }

        Candidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new CandidatureNotFoundException(candidatureId));

        if (!candidature.getProject().getId().equals(projectId)) {
            throw new InvalidProjectOperationException(
                    "Candidature with id=" + candidatureId + " does not belong to project id=" + projectId);
        }

        candidature.setStatus(CandidatureStatus.ACCEPTED);
        candidature = candidatureRepository.save(candidature);

        rejectRemainingPendingCandidatures(projectId, candidatureId);

        project.setStatus(ProjectStatus.ASSIGNED);
        projectRepository.save(project);

        eventPublisher.publishCandidatureAccepted(
                CandidatureAcceptedEvent.builder()
                        .data(CandidatureAcceptedEvent.Data.builder()
                                .candidatureId(candidature.getId())
                                .projectId(projectId)
                                .professionalId(candidature.getProfessionalId())
                                .companyId(companyId)
                                .build())
                        .build());

        log.info("Candidature accepted: candidatureId={}, projectId={}, professionalId={}",
                candidatureId, projectId, candidature.getProfessionalId());
        return candidatureMapper.toResponse(candidature);
    }

    // =========================================================================
    // Completion
    // =========================================================================

    @Override
    public ProjectResponse completeProject(UUID companyId, UUID projectId) {
        Project project = findProjectById(projectId);
        assertOwnership(project, companyId);

        if (project.getStatus() != ProjectStatus.ASSIGNED && project.getStatus() != ProjectStatus.IN_PROGRESS) {
            throw new InvalidProjectOperationException(
                    "Project with id=" + projectId + " cannot be completed from status=" + project.getStatus());
        }

        Candidature accepted = candidatureRepository.findFirstByProjectIdAndStatus(projectId, CandidatureStatus.ACCEPTED)
                .orElseThrow(() -> new InvalidProjectOperationException(
                        "Project with id=" + projectId + " has no accepted candidature"));

        project.setStatus(ProjectStatus.COMPLETED);
        project = projectRepository.save(project);

        eventPublisher.publishProjectCompleted(
                ProjectCompletedEvent.builder()
                        .data(ProjectCompletedEvent.Data.builder()
                                .projectId(project.getId())
                                .companyId(companyId)
                                .professionalId(accepted.getProfessionalId())
                                .build())
                        .build());

        log.info("Project completed: projectId={}", projectId);
        return toResponseWithRequirements(project, requirementRepository.findByProjectId(projectId));
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void rejectRemainingPendingCandidatures(UUID projectId, UUID acceptedCandidatureId) {
        candidatureRepository.findByProjectId(projectId).stream()
                .filter(c -> !c.getId().equals(acceptedCandidatureId) && c.getStatus() == CandidatureStatus.PENDING)
                .forEach(c -> {
                    c.setStatus(CandidatureStatus.REJECTED);
                    candidatureRepository.save(c);
                });
    }

    private ProjectResponse toResponseWithRequirements(Project project, List<ProjectRequirement> requirements) {
        ProjectResponse response = projectMapper.toResponse(project);
        response.setRequirements(requirements.stream()
                .map(requirementMapper::toResponse)
                .collect(Collectors.toList()));
        return response;
    }

    private void assertOwnership(Project project, UUID companyId) {
        if (!project.getCompanyId().equals(companyId)) {
            throw new InvalidProjectOperationException(
                    "Company with id=" + companyId + " is not the owner of project id=" + project.getId());
        }
    }

    private Project findProjectById(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }
}
