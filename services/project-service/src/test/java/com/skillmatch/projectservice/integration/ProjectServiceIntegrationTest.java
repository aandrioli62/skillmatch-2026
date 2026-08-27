package com.skillmatch.projectservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatch.projectservice.client.UserServiceClient;
import com.skillmatch.projectservice.client.UserStatusResponse;
import com.skillmatch.projectservice.config.RabbitMQConfig;
import com.skillmatch.projectservice.dto.request.CandidatureRequest;
import com.skillmatch.projectservice.dto.request.ProjectCreateRequest;
import com.skillmatch.projectservice.dto.request.ProjectRequirementRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the Project Service running against real PostgreSQL and RabbitMQ
 * instances (Testcontainers), exercising the full publish -> apply -> accept -> complete
 * flow described in the roadmap's Fase 2 checkpoints. UserServiceClient is mocked since
 * it represents a synchronous call to a different microservice (User Service), which is
 * outside this service's own boundary and not something Testcontainers should stand up.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class ProjectServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("projectdb")
            .withUsername("skillmatch")
            .withPassword("secret");

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3-management");

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
    }

    private static final GrantedAuthority COMPANY = () -> "ROLE_COMPANY";
    private static final GrantedAuthority PROFESSIONAL = () -> "ROLE_PROFESSIONAL";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private AmqpAdmin rabbitAdmin;

    @MockBean
    private UserServiceClient userServiceClient;

    private String eventsQueueName;

    @BeforeEach
    void bindTestQueueToDomainEvents() {
        Queue queue = QueueBuilder.durable("test.project-events." + UUID.randomUUID()).build();
        TopicExchange exchange = new TopicExchange(RabbitMQConfig.EXCHANGE, true, false);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with("#");
        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(binding);
        eventsQueueName = queue.getName();
    }

    @AfterEach
    void deleteTestQueue() {
        rabbitAdmin.deleteQueue(eventsQueueName);
    }

    private String receiveEventType() throws Exception {
        Message message = rabbitTemplate.receive(eventsQueueName, 5000);
        assertThat(message).isNotNull();
        return objectMapper.readTree(message.getBody()).get("eventType").asText();
    }

    private ProjectCreateRequest createRequest(String title) {
        ProjectRequirementRequest requirement = new ProjectRequirementRequest();
        requirement.setSkillName("Figma");
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setTitle(title);
        request.setBudget(BigDecimal.valueOf(1500));
        request.setRequirements(List.of(requirement));
        return request;
    }

    private UserStatusResponse validatedProfessional() {
        UserStatusResponse status = new UserStatusResponse();
        status.setRole("PROFESSIONAL");
        status.setStatus("VALIDATED");
        return status;
    }

    @Test
    void fullProjectLifecycle_publishApplyAcceptComplete() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();

        when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);

        String createResponse = mockMvc.perform(post("/api/v1/projects")
                        .with(jwt().authorities(COMPANY))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("Consulenza UI/UX"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        UUID projectId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());

        mockMvc.perform(put("/api/v1/projects/{id}/publish", projectId)
                        .with(jwt().authorities(COMPANY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
        assertThat(receiveEventType()).isEqualTo("project.published");

        when(userServiceClient.resolveCurrentUserId()).thenReturn(professionalId);
        when(userServiceClient.getUserStatus(professionalId)).thenReturn(validatedProfessional());

        CandidatureRequest candidatureRequest = new CandidatureRequest();
        candidatureRequest.setCoverLetter("Sono interessato");
        String applyResponse = mockMvc.perform(post("/api/v1/projects/{id}/candidatures", projectId)
                        .with(jwt().authorities(PROFESSIONAL))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(candidatureRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        UUID candidatureId = UUID.fromString(objectMapper.readTree(applyResponse).get("id").asText());

        // A second candidature for the same pair is rejected outright.
        when(userServiceClient.resolveCurrentUserId()).thenReturn(professionalId);
        mockMvc.perform(post("/api/v1/projects/{id}/candidatures", projectId)
                        .with(jwt().authorities(PROFESSIONAL))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(candidatureRequest)))
                .andExpect(status().isUnprocessableEntity());

        when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
        mockMvc.perform(put("/api/v1/projects/{id}/candidatures/{cid}/accept", projectId, candidatureId)
                        .with(jwt().authorities(COMPANY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        assertThat(receiveEventType()).isEqualTo("candidature.accepted");

        mockMvc.perform(put("/api/v1/projects/{id}/complete", projectId)
                        .with(jwt().authorities(COMPANY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        assertThat(receiveEventType()).isEqualTo("project.completed");
    }

    @Test
    void applyToProject_rejectsWhenProjectNotOpen() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();

        when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
        String createResponse = mockMvc.perform(post("/api/v1/projects")
                        .with(jwt().authorities(COMPANY))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("Ancora in bozza"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID projectId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());

        when(userServiceClient.resolveCurrentUserId()).thenReturn(professionalId);
        CandidatureRequest candidatureRequest = new CandidatureRequest();

        mockMvc.perform(post("/api/v1/projects/{id}/candidatures", projectId)
                        .with(jwt().authorities(PROFESSIONAL))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(candidatureRequest)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void applyToProject_rejectsWhenProfessionalNotValidated() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();

        when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
        String createResponse = mockMvc.perform(post("/api/v1/projects")
                        .with(jwt().authorities(COMPANY))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("Progetto aperto"))))
                .andReturn().getResponse().getContentAsString();
        UUID projectId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());

        mockMvc.perform(put("/api/v1/projects/{id}/publish", projectId).with(jwt().authorities(COMPANY)))
                .andExpect(status().isOk());
        receiveEventType(); // drain project.published

        when(userServiceClient.resolveCurrentUserId()).thenReturn(professionalId);
        UserStatusResponse pending = new UserStatusResponse();
        pending.setRole("PROFESSIONAL");
        pending.setStatus("PENDING");
        when(userServiceClient.getUserStatus(professionalId)).thenReturn(pending);

        CandidatureRequest candidatureRequest = new CandidatureRequest();
        mockMvc.perform(post("/api/v1/projects/{id}/candidatures", projectId)
                        .with(jwt().authorities(PROFESSIONAL))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(candidatureRequest)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void acceptCandidature_rejectsWhenCallerIsNotOwner() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID otherCompanyId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();

        when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
        String createResponse = mockMvc.perform(post("/api/v1/projects")
                        .with(jwt().authorities(COMPANY))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("Progetto altrui"))))
                .andReturn().getResponse().getContentAsString();
        UUID projectId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());

        mockMvc.perform(put("/api/v1/projects/{id}/publish", projectId).with(jwt().authorities(COMPANY)))
                .andExpect(status().isOk());
        receiveEventType(); // drain project.published

        when(userServiceClient.resolveCurrentUserId()).thenReturn(professionalId);
        when(userServiceClient.getUserStatus(professionalId)).thenReturn(validatedProfessional());
        CandidatureRequest candidatureRequest = new CandidatureRequest();
        String applyResponse = mockMvc.perform(post("/api/v1/projects/{id}/candidatures", projectId)
                        .with(jwt().authorities(PROFESSIONAL))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(candidatureRequest)))
                .andReturn().getResponse().getContentAsString();
        UUID candidatureId = UUID.fromString(objectMapper.readTree(applyResponse).get("id").asText());

        when(userServiceClient.resolveCurrentUserId()).thenReturn(otherCompanyId);
        mockMvc.perform(put("/api/v1/projects/{id}/candidatures/{cid}/accept", projectId, candidatureId)
                        .with(jwt().authorities(COMPANY)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createProject_requiresCompanyRole() throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                        .with(jwt().authorities(PROFESSIONAL))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("Non autorizzato"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/projects")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("Senza token"))))
                .andExpect(status().isUnauthorized());
    }
}
