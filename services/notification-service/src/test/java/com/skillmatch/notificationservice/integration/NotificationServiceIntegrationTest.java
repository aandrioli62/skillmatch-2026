package com.skillmatch.notificationservice.integration;

import com.skillmatch.notificationservice.client.UserServiceClient;
import com.skillmatch.notificationservice.config.RabbitMQConfig;
import com.skillmatch.notificationservice.model.Notification;
import com.skillmatch.notificationservice.repository.NotificationRepository;
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
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of Notification Service running against real MongoDB and RabbitMQ
 * instances (Testcontainers), exercising the roadmap's Fase 5 checkpoint: every
 * meaningful system action (registration, candidature, payment...) produces a
 * notification visible in the database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class NotificationServiceIntegrationTest {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3-management");

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> MONGO.getReplicaSetUrl("notificationdb"));
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
    }

    private static final GrantedAuthority PROFESSIONAL = () -> "ROLE_PROFESSIONAL";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    private void publish(String routingKey, String eventType, Map<String, Object> data) {
        Map<String, Object> event = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", eventType,
                "source", "test",
                "data", data);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, routingKey, event);
    }

    @Test
    void userRegistered_producesOneNotificationForTheUser() {
        UUID userId = UUID.randomUUID();
        publish("user.registered", "user.registered", Map.of("userId", userId.toString(), "email", "test@skillmatch.test"));

        List<Notification> found = await().atMost(Duration.ofSeconds(10))
                .until(() -> notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId),
                        list -> !list.isEmpty());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getEventType()).isEqualTo("user.registered");
        assertThat(found.get(0).getMessage()).containsIgnoringCase("benvenuto");
    }

    @Test
    void candidatureAccepted_producesNotificationsForBothParties() {
        UUID professionalId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        publish("candidature.accepted", "candidature.accepted", Map.of(
                "professionalId", professionalId.toString(),
                "companyId", companyId.toString()));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> notificationRepository.findByRecipientIdOrderByCreatedAtDesc(professionalId),
                        list -> !list.isEmpty());

        List<Notification> professionalNotifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(professionalId);
        List<Notification> companyNotifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(companyId);
        assertThat(professionalNotifications).hasSize(1);
        assertThat(companyNotifications).hasSize(1);
    }

    @Test
    void paymentCompleted_thenGetMine_returnsNotificationViaRestApi() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        publish("payment.completed", "payment.completed", Map.of(
                "companyId", companyId.toString(),
                "professionalId", professionalId.toString(),
                "totalAmount", 1000));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> notificationRepository.findByRecipientIdOrderByCreatedAtDesc(professionalId),
                        list -> !list.isEmpty());

        when(userServiceClient.resolveCurrentUserId()).thenReturn(professionalId);
        mockMvc.perform(get("/api/v1/notifications/mine").with(jwt().authorities(PROFESSIONAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("payment.completed"));
    }

    @Test
    void unknownEventType_stillPersistsAGenericNotification() {
        publish("some.new.thing", "some.new.thing", Map.of("foo", "bar"));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> notificationRepository.findAll().stream()
                        .anyMatch(n -> "some.new.thing".equals(n.getEventType())));
    }
}
