package com.skillmatch.userservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatch.userservice.config.RabbitMQConfig;
import com.skillmatch.userservice.dto.request.CompanyProfileRequest;
import com.skillmatch.userservice.dto.request.ProfessionalProfileRequest;
import com.skillmatch.userservice.dto.request.UserRegistrationRequest;
import com.skillmatch.userservice.model.Skill;
import com.skillmatch.userservice.model.User;
import com.skillmatch.userservice.model.UserSkill;
import com.skillmatch.userservice.model.UserSkillId;
import com.skillmatch.userservice.model.enums.UserRole;
import com.skillmatch.userservice.repository.SkillRepository;
import com.skillmatch.userservice.repository.UserRepository;
import com.skillmatch.userservice.repository.UserSkillRepository;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the User Service running against real PostgreSQL and RabbitMQ
 * instances (Testcontainers), exercising the full registration -> profile -> admin
 * validation flow described in the roadmap's Fase 1 checkpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class UserServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("userdb")
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

    private static final GrantedAuthority ADMIN = () -> "ROLE_ADMIN";
    private static final GrantedAuthority PROFESSIONAL = () -> "ROLE_PROFESSIONAL";
    private static final GrantedAuthority COMPANY = () -> "ROLE_COMPANY";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private AmqpAdmin rabbitAdmin;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private UserSkillRepository userSkillRepository;

    private String eventsQueueName;

    @BeforeEach
    void bindTestQueueToDomainEvents() {
        // Bound *before* any action in the test so events published during the test are captured.
        Queue queue = QueueBuilder.durable("test.user-events." + UUID.randomUUID()).autoDelete().build();
        TopicExchange exchange = new TopicExchange(RabbitMQConfig.EXCHANGE, true, false);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with("user.*");
        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(binding);
        eventsQueueName = queue.getName();
    }

    private String receiveEventType() throws Exception {
        Message message = rabbitTemplate.receive(eventsQueueName, 5000);
        assertThat(message).isNotNull();
        return objectMapper.readTree(message.getBody()).get("eventType").asText();
    }

    private UserRegistrationRequest registrationRequest(UserRole role, String email) {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setKeycloakId("kc-" + UUID.randomUUID());
        request.setEmail(email);
        request.setRole(role);
        return request;
    }

    @Test
    void fullProfessionalLifecycle_registerThenValidateThenUpdateProfile() throws Exception {
        UserRegistrationRequest registration = registrationRequest(UserRole.PROFESSIONAL, "ada@example.com");

        String registerResponse = mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        UUID userId = UUID.fromString(objectMapper.readTree(registerResponse).get("id").asText());

        assertThat(receiveEventType()).isEqualTo("user.registered");

        // A registered PROFESSIONAL cannot yet apply — must be validated by an admin first.
        mockMvc.perform(post("/api/v1/admin/users/{id}/validate", userId).with(jwt().authorities(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"));

        assertThat(receiveEventType()).isEqualTo("user.validated");

        // Re-validating the same professional is rejected.
        mockMvc.perform(post("/api/v1/admin/users/{id}/validate", userId).with(jwt().authorities(ADMIN)))
                .andExpect(status().isUnprocessableEntity());

        ProfessionalProfileRequest profileRequest = new ProfessionalProfileRequest();
        profileRequest.setFirstName("Ada");
        profileRequest.setLastName("Lovelace");
        profileRequest.setBio("Pioneering programmer");

        mockMvc.perform(put("/api/v1/users/{id}/professional-profile", userId)
                        .with(jwt().authorities(PROFESSIONAL))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(profileRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ada"));

        // Give the now-validated professional a skill and confirm the custom search query finds them.
        Skill java = skillRepository.findByNameIgnoreCase("Java")
                .orElseGet(() -> {
                    Skill s = new Skill();
                    s.setName("Java");
                    s.setCategory("Backend");
                    return skillRepository.save(s);
                });
        User user = userRepository.findById(userId).orElseThrow();
        UserSkill userSkill = new UserSkill();
        userSkill.setId(new UserSkillId(userId, java.getId()));
        userSkill.setUser(user);
        userSkill.setSkill(java);
        userSkillRepository.save(userSkill);

        mockMvc.perform(get("/api/v1/users/professionals/search")
                        .param("skill", "java")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Ada"));
    }

    @Test
    void companyRegistrationAndProfileUpdate() throws Exception {
        UserRegistrationRequest registration = registrationRequest(UserRole.COMPANY, "acme@example.com");

        String response = mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID userId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

        CompanyProfileRequest companyRequest = new CompanyProfileRequest();
        companyRequest.setCompanyName("Acme Corp");
        companyRequest.setVatNumber("IT12345678901");

        mockMvc.perform(put("/api/v1/users/{id}/company-profile", userId)
                        .with(jwt().authorities(COMPANY))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(companyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Acme Corp"));

        // A COMPANY account cannot receive a professional profile — rejected by role before it
        // would even reach the business-rule check, since the caller lacks ROLE_PROFESSIONAL.
        ProfessionalProfileRequest validProfessionalBody = new ProfessionalProfileRequest();
        validProfessionalBody.setFirstName("Not");
        validProfessionalBody.setLastName("Allowed");

        mockMvc.perform(put("/api/v1/users/{id}/professional-profile", userId)
                        .with(jwt().authorities(PROFESSIONAL))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validProfessionalBody)))
                .andExpect(status().isForbidden());
    }

    @Test
    void registeringTheSameEmailTwiceIsRejected() throws Exception {
        UserRegistrationRequest registration = registrationRequest(UserRole.PROFESSIONAL, "duplicate@example.com");

        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isCreated());

        UserRegistrationRequest secondAttempt = registrationRequest(UserRole.PROFESSIONAL, "duplicate@example.com");
        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(secondAttempt)))
                .andExpect(status().isConflict());
    }

    @Test
    void adminEndpointsRejectNonAdminCallers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").with(jwt().authorities(PROFESSIONAL)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }
}
