package com.skillmatch.projectservice.service;

import com.skillmatch.projectservice.client.UserServiceClient;
import com.skillmatch.projectservice.client.UserStatusResponse;
import com.skillmatch.projectservice.dto.request.CandidatureRequest;
import com.skillmatch.projectservice.dto.request.ProjectCreateRequest;
import com.skillmatch.projectservice.dto.request.ProjectRequirementRequest;
import com.skillmatch.projectservice.dto.response.CandidatureResponse;
import com.skillmatch.projectservice.dto.response.ProjectRequirementResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectServiceImpl — Unit Tests")
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectRequirementRepository requirementRepository;
    @Mock
    private CandidatureRepository candidatureRepository;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private EventPublisherService eventPublisher;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ProjectRequirementMapper requirementMapper;
    @Mock
    private CandidatureMapper candidatureMapper;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private UUID companyId;
    private UUID projectId;
    private Project draftProject;
    private Project openProject;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        draftProject = new Project();
        draftProject.setId(projectId);
        draftProject.setCompanyId(companyId);
        draftProject.setTitle("Consulenza UI/UX");
        draftProject.setStatus(ProjectStatus.DRAFT);

        openProject = new Project();
        openProject.setId(projectId);
        openProject.setCompanyId(companyId);
        openProject.setTitle("Consulenza UI/UX");
        openProject.setStatus(ProjectStatus.OPEN);
        openProject.setBudget(BigDecimal.valueOf(1500));
    }

    // =========================================================================
    // createProject
    // =========================================================================

    @Nested
    @DisplayName("createProject()")
    class CreateProject {

        @Test
        @DisplayName("saves project with companyId set, saves each requirement linked to it")
        void createProject_success() {
            ProjectRequirementRequest reqDto = new ProjectRequirementRequest();
            reqDto.setSkillName("Figma");
            ProjectCreateRequest request = new ProjectCreateRequest();
            request.setTitle("Consulenza UI/UX");
            request.setRequirements(List.of(reqDto));

            Project newProject = new Project();
            ProjectRequirement newRequirement = new ProjectRequirement();
            ProjectResponse response = new ProjectResponse();

            when(projectMapper.toEntity(request)).thenReturn(newProject);
            when(projectRepository.save(newProject)).thenReturn(draftProject);
            when(requirementMapper.toEntity(reqDto)).thenReturn(newRequirement);
            when(requirementRepository.save(newRequirement)).thenReturn(newRequirement);
            when(projectMapper.toResponse(draftProject)).thenReturn(response);
            when(requirementMapper.toResponse(newRequirement)).thenReturn(new ProjectRequirementResponse());

            ProjectResponse result = projectService.createProject(companyId, request);

            assertThat(result).isSameAs(response);
            assertThat(newProject.getCompanyId()).isEqualTo(companyId);
            assertThat(newRequirement.getProject()).isSameAs(draftProject);
            verifyNoInteractions(eventPublisher);
        }
    }

    // =========================================================================
    // publishProject
    // =========================================================================

    @Nested
    @DisplayName("publishProject()")
    class PublishProject {

        @Test
        @DisplayName("DRAFT project owned by caller: transitions to OPEN and publishes project.published")
        void publishProject_success() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(draftProject));
            when(projectRepository.save(draftProject)).thenReturn(draftProject);

            ProjectRequirement requirement = new ProjectRequirement();
            requirement.setSkillName("Figma");
            when(requirementRepository.findByProjectId(projectId)).thenReturn(List.of(requirement));
            when(projectMapper.toResponse(draftProject)).thenReturn(new ProjectResponse());
            when(requirementMapper.toResponse(requirement)).thenReturn(new ProjectRequirementResponse());

            projectService.publishProject(companyId, projectId);

            assertThat(draftProject.getStatus()).isEqualTo(ProjectStatus.OPEN);

            ArgumentCaptor<ProjectPublishedEvent> captor = ArgumentCaptor.forClass(ProjectPublishedEvent.class);
            verify(eventPublisher).publishProjectPublished(captor.capture());
            assertThat(captor.getValue().getData().getProjectId()).isEqualTo(projectId);
            assertThat(captor.getValue().getData().getRequiredSkills()).containsExactly("Figma");
        }

        @Test
        @DisplayName("caller is not the owning company: throws InvalidProjectOperationException")
        void publishProject_notOwner_throws() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(draftProject));

            assertThatThrownBy(() -> projectService.publishProject(UUID.randomUUID(), projectId))
                    .isInstanceOf(InvalidProjectOperationException.class)
                    .hasMessageContaining("not the owner");

            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("project not in DRAFT status: throws InvalidProjectOperationException")
        void publishProject_notDraft_throws() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(openProject));

            assertThatThrownBy(() -> projectService.publishProject(companyId, projectId))
                    .isInstanceOf(InvalidProjectOperationException.class)
                    .hasMessageContaining("cannot be published");
        }

        @Test
        @DisplayName("unknown project: throws ProjectNotFoundException")
        void publishProject_notFound_throws() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.publishProject(companyId, projectId))
                    .isInstanceOf(ProjectNotFoundException.class);
        }
    }

    // =========================================================================
    // getProject / listOpenProjects / listProjectsByCompany
    // =========================================================================

    @Nested
    @DisplayName("getProject()")
    class GetProject {

        @Test
        @DisplayName("existing project: returns mapped response with requirements")
        void getProject_found() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(openProject));
            when(requirementRepository.findByProjectId(projectId)).thenReturn(List.of());
            when(projectMapper.toResponse(openProject)).thenReturn(new ProjectResponse());

            ProjectResponse result = projectService.getProject(projectId);

            assertThat(result).isNotNull();
            assertThat(result.getRequirements()).isEmpty();
        }

        @Test
        @DisplayName("unknown project: throws ProjectNotFoundException")
        void getProject_notFound() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.getProject(projectId))
                    .isInstanceOf(ProjectNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listOpenProjects() / listProjectsByCompany()")
    class Listing {

        @Test
        @DisplayName("listOpenProjects: returns only OPEN projects mapped")
        void listOpenProjects_returnsMapped() {
            when(projectRepository.findByStatus(ProjectStatus.OPEN)).thenReturn(List.of(openProject));
            when(requirementRepository.findByProjectId(projectId)).thenReturn(List.of());
            when(projectMapper.toResponse(openProject)).thenReturn(new ProjectResponse());

            List<ProjectResponse> result = projectService.listOpenProjects();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("listProjectsByCompany: returns all projects owned by the company")
        void listProjectsByCompany_returnsMapped() {
            when(projectRepository.findByCompanyId(companyId)).thenReturn(List.of(draftProject, openProject));
            when(requirementRepository.findByProjectId(any())).thenReturn(List.of());
            when(projectMapper.toResponse(any())).thenReturn(new ProjectResponse());

            List<ProjectResponse> result = projectService.listProjectsByCompany(companyId);

            assertThat(result).hasSize(2);
        }
    }

    // =========================================================================
    // listCandidaturesByProfessional
    // =========================================================================

    @Nested
    @DisplayName("listCandidaturesByProfessional()")
    class ListCandidaturesByProfessional {

        @Test
        @DisplayName("returns all candidatures submitted by the professional, mapped")
        void listCandidaturesByProfessional_returnsMapped() {
            UUID professionalId = UUID.randomUUID();
            Candidature candidature = new Candidature();

            when(candidatureRepository.findByProfessionalId(professionalId)).thenReturn(List.of(candidature));
            when(candidatureMapper.toResponse(candidature)).thenReturn(new CandidatureResponse());

            List<CandidatureResponse> result = projectService.listCandidaturesByProfessional(professionalId);

            assertThat(result).hasSize(1);
        }
    }

    // =========================================================================
    // applyToProject
    // =========================================================================

    @Nested
    @DisplayName("applyToProject()")
    class ApplyToProject {

        private UUID professionalId;
        private CandidatureRequest request;

        @BeforeEach
        void setUpCandidature() {
            professionalId = UUID.randomUUID();
            request = new CandidatureRequest();
            request.setCoverLetter("Sono interessato");
        }

        @Test
        @DisplayName("OPEN project, VALIDATED professional, no prior candidature: saves PENDING candidature")
        void applyToProject_success() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(openProject));
            UserStatusResponse status = new UserStatusResponse();
            status.setRole("PROFESSIONAL");
            status.setStatus("VALIDATED");
            when(userServiceClient.getUserStatus(professionalId)).thenReturn(status);
            when(candidatureRepository.findByProjectIdAndProfessionalId(projectId, professionalId))
                    .thenReturn(Optional.empty());

            Candidature newCandidature = new Candidature();
            when(candidatureMapper.toEntity(request)).thenReturn(newCandidature);
            when(candidatureRepository.save(newCandidature)).thenReturn(newCandidature);
            when(candidatureMapper.toResponse(newCandidature)).thenReturn(new CandidatureResponse());

            projectService.applyToProject(professionalId, projectId, request);

            assertThat(newCandidature.getProject()).isSameAs(openProject);
            assertThat(newCandidature.getProfessionalId()).isEqualTo(professionalId);
        }

        @Test
        @DisplayName("project not OPEN: throws InvalidProjectOperationException without calling User Service")
        void applyToProject_projectNotOpen_throws() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(draftProject));

            assertThatThrownBy(() -> projectService.applyToProject(professionalId, projectId, request))
                    .isInstanceOf(InvalidProjectOperationException.class)
                    .hasMessageContaining("not open");

            verifyNoInteractions(userServiceClient);
        }

        @Test
        @DisplayName("caller is not a PROFESSIONAL: throws InvalidProjectOperationException")
        void applyToProject_notProfessional_throws() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(openProject));
            UserStatusResponse status = new UserStatusResponse();
            status.setRole("COMPANY");
            status.setStatus("VALIDATED");
            when(userServiceClient.getUserStatus(professionalId)).thenReturn(status);

            assertThatThrownBy(() -> projectService.applyToProject(professionalId, projectId, request))
                    .isInstanceOf(InvalidProjectOperationException.class)
                    .hasMessageContaining("is not a PROFESSIONAL");

            verifyNoInteractions(candidatureRepository);
        }

        @Test
        @DisplayName("professional not VALIDATED: throws InvalidProjectOperationException")
        void applyToProject_notValidated_throws() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(openProject));
            UserStatusResponse status = new UserStatusResponse();
            status.setRole("PROFESSIONAL");
            status.setStatus("PENDING");
            when(userServiceClient.getUserStatus(professionalId)).thenReturn(status);

            assertThatThrownBy(() -> projectService.applyToProject(professionalId, projectId, request))
                    .isInstanceOf(InvalidProjectOperationException.class)
                    .hasMessageContaining("must be VALIDATED");
        }

        @Test
        @DisplayName("professional already applied: throws InvalidProjectOperationException")
        void applyToProject_alreadyApplied_throws() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(openProject));
            UserStatusResponse status = new UserStatusResponse();
            status.setRole("PROFESSIONAL");
            status.setStatus("VALIDATED");
            when(userServiceClient.getUserStatus(professionalId)).thenReturn(status);
            when(candidatureRepository.findByProjectIdAndProfessionalId(projectId, professionalId))
                    .thenReturn(Optional.of(new Candidature()));

            assertThatThrownBy(() -> projectService.applyToProject(professionalId, projectId, request))
                    .isInstanceOf(InvalidProjectOperationException.class)
                    .hasMessageContaining("already applied");

            verify(candidatureRepository, never()).save(any());
        }
    }

    // =========================================================================
    // acceptCandidature
    // =========================================================================

    @Nested
    @DisplayName("acceptCandidature()")
    class AcceptCandidature {

        private UUID candidatureId;
        private Candidature pendingCandidature;

        @BeforeEach
        void setUpCandidature() {
            candidatureId = UUID.randomUUID();
            pendingCandidature = new Candidature();
            pendingCandidature.setId(candidatureId);
            pendingCandidature.setProject(openProject);
            pendingCandidature.setProfessionalId(UUID.randomUUID());
            pendingCandidature.setStatus(CandidatureStatus.PENDING);
        }

        @Test
        @DisplayName("accepts candidature, rejects other PENDING ones, assigns project, publishes event")
        void acceptCandidature_success() {
            Candidature otherPending = new Candidature();
            otherPending.setId(UUID.randomUUID());
            otherPending.setStatus(CandidatureStatus.PENDING);

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(openProject));
            when(candidatureRepository.existsByProjectIdAndStatus(projectId, CandidatureStatus.ACCEPTED))
                    .thenReturn(false);
            when(candidatureRepository.findById(candidatureId)).thenReturn(Optional.of(pendingCandidature));
            when(candidatureRepository.save(pendingCandidature)).thenReturn(pendingCandidature);
            when(candidatureRepository.findByProjectId(projectId)).thenReturn(List.of(pendingCandidature, otherPending));
            when(projectRepository.save(openProject)).thenReturn(openProject);
            when(candidatureMapper.toResponse(pendingCandidature)).thenReturn(new CandidatureResponse());

            projectService.acceptCandidature(companyId, projectId, candidatureId);

            assertThat(pendingCandidature.getStatus()).isEqualTo(CandidatureStatus.ACCEPTED);
            assertThat(otherPending.getStatus()).isEqualTo(CandidatureStatus.REJECTED);
            assertThat(openProject.getStatus()).isEqualTo(ProjectStatus.ASSIGNED);
            verify(candidatureRepository).save(otherPending);

            ArgumentCaptor<CandidatureAcceptedEvent> captor = ArgumentCaptor.forClass(CandidatureAcceptedEvent.class);
            verify(eventPublisher).publishCandidatureAccepted(captor.capture());
            assertThat(captor.getValue().getData().getCandidatureId()).isEqualTo(candidatureId);
            assertThat(captor.getValue().getData().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        }

        @Test
        @DisplayName("caller is not the owning company: throws InvalidProjectOperationException")
        void acceptCandidature_notOwner_throws() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(openProject));

            assertThatThrownBy(() -> projectService.acceptCandidature(UUID.randomUUID(), projectId, candidatureId))
                    .isInstanceOf(InvalidProjectOperationException.class)
                    .hasMessageContaining("not the owner");
        }

        @Test
        @DisplayName("project already has an accepted candidature: throws InvalidProjectOperationException")
        void acceptCandidature_alreadyAccepted_throws() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(openProject));
            when(candidatureRepository.existsByProjectIdAndStatus(projectId, CandidatureStatus.ACCEPTED))
                    .thenReturn(true);

            assertThatThrownBy(() -> projectService.acceptCandidature(companyId, projectId, candidatureId))
                    .isInstanceOf(InvalidProjectOperationException.class)
                    .hasMessageContaining("already has an accepted candidature");

            verify(candidatureRepository, never()).findById(any());
        }

        @Test
        @DisplayName("unknown candidature: throws CandidatureNotFoundException")
        void acceptCandidature_notFound_throws() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(openProject));
            when(candidatureRepository.existsByProjectIdAndStatus(projectId, CandidatureStatus.ACCEPTED))
                    .thenReturn(false);
            when(candidatureRepository.findById(candidatureId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.acceptCandidature(companyId, projectId, candidatureId))
                    .isInstanceOf(CandidatureNotFoundException.class);
        }

        @Test
        @DisplayName("candidature belongs to a different project: throws InvalidProjectOperationException")
        void acceptCandidature_wrongProject_throws() {
            Project otherProject = new Project();
            otherProject.setId(UUID.randomUUID());
            pendingCandidature.setProject(otherProject);

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(openProject));
            when(candidatureRepository.existsByProjectIdAndStatus(projectId, CandidatureStatus.ACCEPTED))
                    .thenReturn(false);
            when(candidatureRepository.findById(candidatureId)).thenReturn(Optional.of(pendingCandidature));

            assertThatThrownBy(() -> projectService.acceptCandidature(companyId, projectId, candidatureId))
                    .isInstanceOf(InvalidProjectOperationException.class)
                    .hasMessageContaining("does not belong to project");
        }
    }

    // =========================================================================
    // completeProject
    // =========================================================================

    @Nested
    @DisplayName("completeProject()")
    class CompleteProject {

        @Test
        @DisplayName("ASSIGNED project with accepted candidature: transitions to COMPLETED and publishes event")
        void completeProject_success() {
            openProject.setStatus(ProjectStatus.ASSIGNED);
            UUID professionalId = UUID.randomUUID();
            Candidature accepted = new Candidature();
            accepted.setProfessionalId(professionalId);
            accepted.setStatus(CandidatureStatus.ACCEPTED);

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(openProject));
            when(candidatureRepository.findFirstByProjectIdAndStatus(projectId, CandidatureStatus.ACCEPTED))
                    .thenReturn(Optional.of(accepted));
            when(projectRepository.save(openProject)).thenReturn(openProject);
            when(requirementRepository.findByProjectId(projectId)).thenReturn(List.of());
            when(projectMapper.toResponse(openProject)).thenReturn(new ProjectResponse());

            projectService.completeProject(companyId, projectId);

            assertThat(openProject.getStatus()).isEqualTo(ProjectStatus.COMPLETED);

            ArgumentCaptor<ProjectCompletedEvent> captor = ArgumentCaptor.forClass(ProjectCompletedEvent.class);
            verify(eventPublisher).publishProjectCompleted(captor.capture());
            assertThat(captor.getValue().getData().getProfessionalId()).isEqualTo(professionalId);
        }

        @Test
        @DisplayName("caller is not the owning company: throws InvalidProjectOperationException")
        void completeProject_notOwner_throws() {
            openProject.setStatus(ProjectStatus.ASSIGNED);
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(openProject));

            assertThatThrownBy(() -> projectService.completeProject(UUID.randomUUID(), projectId))
                    .isInstanceOf(InvalidProjectOperationException.class)
                    .hasMessageContaining("not the owner");
        }

        @Test
        @DisplayName("project still OPEN (no candidature accepted yet): throws InvalidProjectOperationException")
        void completeProject_wrongStatus_throws() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(openProject));

            assertThatThrownBy(() -> projectService.completeProject(companyId, projectId))
                    .isInstanceOf(InvalidProjectOperationException.class)
                    .hasMessageContaining("cannot be completed");

            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("ASSIGNED but no accepted candidature found: throws InvalidProjectOperationException")
        void completeProject_noAcceptedCandidature_throws() {
            openProject.setStatus(ProjectStatus.ASSIGNED);
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(openProject));
            when(candidatureRepository.findFirstByProjectIdAndStatus(projectId, CandidatureStatus.ACCEPTED))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.completeProject(companyId, projectId))
                    .isInstanceOf(InvalidProjectOperationException.class)
                    .hasMessageContaining("no accepted candidature");
        }
    }
}
