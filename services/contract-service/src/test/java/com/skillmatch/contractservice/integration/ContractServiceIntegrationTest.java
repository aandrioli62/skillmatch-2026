package com.skillmatch.contractservice.integration;

import com.skillmatch.contractservice.client.UserServiceClient;
import com.skillmatch.contractservice.config.RabbitMQConfig;
import com.skillmatch.contractservice.model.Contract;
import com.skillmatch.contractservice.model.enums.ContractStatus;
import com.skillmatch.contractservice.repository.ContractRepository;
import org.junit.jupiter.api.Test;
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
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of Contract Service running against real PostgreSQL and RabbitMQ
 * instances (Testcontainers). Publishes a candidature.accepted event onto the shared
 * skillmatch.events exchange exactly as project-service would, and exercises the
 * resulting sign -> sign -> complete flow described in the roadmap's Fase 4.1
 * checkpoint. UserServiceClient is mocked since it represents a synchronous call to
 * a different microservice (User Service), outside this service's own boundary.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class ContractServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("contractdb")
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
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ContractRepository contractRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    /**
     * Publishes a raw JSON-shaped payload (a Map, not project-service's Java class) onto
     * the shared exchange — mirroring the fact that these are two independently deployed
     * services that only agree on a JSON contract, not a shared Java type.
     */
    private void publishCandidatureAccepted(UUID candidatureId, UUID projectId, UUID professionalId,
                                             UUID companyId, BigDecimal amount) {
        Map<String, Object> data = Map.of(
                "candidatureId", candidatureId.toString(),
                "projectId", projectId.toString(),
                "professionalId", professionalId.toString(),
                "companyId", companyId.toString(),
                "amount", amount);
        Map<String, Object> event = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "candidature.accepted",
                "source", "project-service",
                "data", data);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "candidature.accepted", event);
    }

    @Test
    void candidatureAccepted_createsContract_thenSignSignComplete() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        publishCandidatureAccepted(UUID.randomUUID(), projectId, professionalId, companyId, BigDecimal.valueOf(1500));

        Contract created = await().atMost(Duration.ofSeconds(10))
                .until(() -> contractRepository.findByProjectId(projectId).orElse(null), c -> c != null);
        assertThat(created.getStatus()).isEqualTo(ContractStatus.DRAFT);
        assertThat(created.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(created.getCommissionRate()).isEqualByComparingTo(new BigDecimal("8.00"));
        UUID contractId = created.getId();

        when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
        mockMvc.perform(put("/api/v1/contracts/{id}/sign", contractId).with(jwt().authorities(COMPANY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_SIGNATURES"));

        when(userServiceClient.resolveCurrentUserId()).thenReturn(professionalId);
        mockMvc.perform(put("/api/v1/contracts/{id}/sign", contractId).with(jwt().authorities(PROFESSIONAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.signedAt").exists());

        when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
        mockMvc.perform(put("/api/v1/contracts/{id}/complete", contractId).with(jwt().authorities(COMPANY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void candidatureAccepted_duplicateEvent_doesNotCreateSecondContract() {
        UUID projectId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        publishCandidatureAccepted(UUID.randomUUID(), projectId, professionalId, companyId, BigDecimal.valueOf(2000));
        await().atMost(Duration.ofSeconds(10))
                .until(() -> contractRepository.findByProjectId(projectId).orElse(null), c -> c != null);

        publishCandidatureAccepted(UUID.randomUUID(), projectId, professionalId, companyId, BigDecimal.valueOf(2000));

        // No second contract should ever appear for the same project — give the (would-be
        // duplicate) consumption a moment to land, then assert the count stayed at one.
        await().pollDelay(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(contractRepository.findByProjectId(projectId)).isPresent());
        assertThat(contractRepository.findAll().stream().filter(c -> c.getProjectId().equals(projectId)).count())
                .isEqualTo(1);
    }

    @Test
    void signContract_wrongParty_returnsUnprocessableEntity() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        publishCandidatureAccepted(UUID.randomUUID(), projectId, professionalId, companyId, BigDecimal.valueOf(800));
        Contract created = await().atMost(Duration.ofSeconds(10))
                .until(() -> contractRepository.findByProjectId(projectId).orElse(null), c -> c != null);

        when(userServiceClient.resolveCurrentUserId()).thenReturn(professionalId);
        mockMvc.perform(put("/api/v1/contracts/{id}/sign", created.getId()).with(jwt().authorities(PROFESSIONAL)))
                .andExpect(status().isUnprocessableEntity());
    }
}
