package com.skillmatch.feedbackservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatch.feedbackservice.client.UserServiceClient;
import com.skillmatch.feedbackservice.config.RabbitMQConfig;
import com.skillmatch.feedbackservice.dto.request.SubmitFeedbackRequest;
import com.skillmatch.feedbackservice.repository.FeedbackEligibilityRepository;
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

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of Feedback Service running against real PostgreSQL and RabbitMQ
 * instances (Testcontainers). Publishes a payment.completed event exactly as
 * payment-service would, then exercises the mutual sign -> submit -> aggregate flow
 * from the roadmap's Fase 4.3 checkpoint. UserServiceClient is mocked since it
 * represents a synchronous call to a different microservice.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class FeedbackServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("feedbackdb")
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
    @Autowired
    private FeedbackEligibilityRepository eligibilityRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    private String eventsQueueName;

    @BeforeEach
    void bindTestQueueToDomainEvents() {
        Queue queue = QueueBuilder.durable("test.feedback-events." + UUID.randomUUID()).build();
        TopicExchange exchange = new TopicExchange(RabbitMQConfig.EXCHANGE, true, false);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with("feedback.aggregated");
        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(binding);
        eventsQueueName = queue.getName();
    }

    @AfterEach
    void deleteTestQueue() {
        rabbitAdmin.deleteQueue(eventsQueueName);
    }

    private void publishPaymentCompleted(UUID projectId, UUID companyId, UUID professionalId) {
        Map<String, Object> data = Map.of(
                "transactionId", UUID.randomUUID().toString(),
                "contractId", UUID.randomUUID().toString(),
                "projectId", projectId.toString(),
                "companyId", companyId.toString(),
                "professionalId", professionalId.toString(),
                "totalAmount", 1000,
                "commissionAmount", 80,
                "netAmount", 920);
        Map<String, Object> event = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "payment.completed",
                "source", "payment-service",
                "data", data);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "payment.completed", event);
    }

    private SubmitFeedbackRequest requestFor(UUID projectId, int rating) {
        SubmitFeedbackRequest request = new SubmitFeedbackRequest();
        request.setProjectId(projectId);
        request.setRating(rating);
        request.setComment("Test");
        return request;
    }

    @Test
    void paymentCompleted_enablesFeedback_thenMutualSubmissionAggregatesReputation() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();

        publishPaymentCompleted(projectId, companyId, professionalId);
        await().atMost(Duration.ofSeconds(10))
                .until(() -> eligibilityRepository.existsByProjectId(projectId));

        when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
        mockMvc.perform(post("/api/v1/feedbacks")
                        .with(jwt().authorities(COMPANY))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestFor(projectId, 5))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.revieweeId").value(professionalId.toString()));

        Message message = rabbitTemplate.receive(eventsQueueName, 5000);
        assertThat(message).isNotNull();
        var payload = objectMapper.readTree(message.getBody());
        assertThat(payload.get("eventType").asText()).isEqualTo("feedback.aggregated");
        assertThat(payload.get("data").get("professionalId").asText()).isEqualTo(professionalId.toString());
        assertThat(payload.get("data").get("avgRating").asDouble()).isEqualTo(5.0);
        assertThat(payload.get("data").get("totalReviews").asInt()).isEqualTo(1);

        when(userServiceClient.resolveCurrentUserId()).thenReturn(professionalId);
        mockMvc.perform(post("/api/v1/feedbacks")
                        .with(jwt().authorities(PROFESSIONAL))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestFor(projectId, 4))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.revieweeId").value(companyId.toString()));

        // professional -> company feedback does not trigger a reputation aggregation
        Message secondMessage = rabbitTemplate.receive(eventsQueueName, 2000);
        assertThat(secondMessage).isNull();
    }

    @Test
    void submitFeedback_rejectsWhenProjectNotEligible() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);

        mockMvc.perform(post("/api/v1/feedbacks")
                        .with(jwt().authorities(COMPANY))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestFor(projectId, 5))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void submitFeedback_rejectsDuplicateReviewForSamePair() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();

        publishPaymentCompleted(projectId, companyId, professionalId);
        await().atMost(Duration.ofSeconds(10))
                .until(() -> eligibilityRepository.existsByProjectId(projectId));

        when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
        mockMvc.perform(post("/api/v1/feedbacks")
                        .with(jwt().authorities(COMPANY))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestFor(projectId, 5))))
                .andExpect(status().isCreated());
        rabbitTemplate.receive(eventsQueueName, 5000); // drain feedback.aggregated

        mockMvc.perform(post("/api/v1/feedbacks")
                        .with(jwt().authorities(COMPANY))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestFor(projectId, 3))))
                .andExpect(status().isUnprocessableEntity());
    }
}
