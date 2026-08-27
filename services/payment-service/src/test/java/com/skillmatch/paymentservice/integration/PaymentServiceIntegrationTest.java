package com.skillmatch.paymentservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatch.paymentservice.client.ContractServiceClient;
import com.skillmatch.paymentservice.client.ContractSummaryResponse;
import com.skillmatch.paymentservice.client.UserServiceClient;
import com.skillmatch.paymentservice.config.RabbitMQConfig;
import com.skillmatch.paymentservice.dto.request.CommissionConfigRequest;
import com.skillmatch.paymentservice.dto.request.InitiatePaymentRequest;
import com.skillmatch.paymentservice.model.CommissionConfig;
import com.skillmatch.paymentservice.repository.CommissionConfigRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of Payment Service running against real PostgreSQL and RabbitMQ
 * instances (Testcontainers), exercising the roadmap's Fase 4.2 checkpoint: complete a
 * contract -> initiate payment -> commission calculated correctly -> payment.completed
 * published. UserServiceClient and ContractServiceClient are mocked since they represent
 * synchronous calls to other microservices, outside this service's own boundary.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class PaymentServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("paymentdb")
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
    private static final GrantedAuthority ADMIN = () -> "ROLE_ADMIN";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private AmqpAdmin rabbitAdmin;
    @Autowired
    private CommissionConfigRepository commissionConfigRepository;

    @MockBean
    private UserServiceClient userServiceClient;
    @MockBean
    private ContractServiceClient contractServiceClient;

    private String eventsQueueName;

    @BeforeEach
    void bindTestQueueToDomainEvents() {
        Queue queue = QueueBuilder.durable("test.payment-events." + UUID.randomUUID()).build();
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

    /**
     * commission_config is shared, un-reset state across every test method in this class
     * (one Testcontainers Postgres instance for the whole class, and JUnit does not
     * guarantee method execution order) — reset it to the seeded 8.00 default after each
     * test so a rate change made by one test can never leak into another.
     */
    @AfterEach
    void resetCommissionConfig() {
        commissionConfigRepository.deleteAll();
        CommissionConfig defaultConfig = new CommissionConfig();
        defaultConfig.setRatePercentage(new BigDecimal("8.00"));
        commissionConfigRepository.save(defaultConfig);
    }

    private String receiveEventType() throws Exception {
        Message message = rabbitTemplate.receive(eventsQueueName, 5000);
        assertThat(message).isNotNull();
        return objectMapper.readTree(message.getBody()).get("eventType").asText();
    }

    private ContractSummaryResponse completedContract(UUID contractId, UUID companyId, UUID professionalId, BigDecimal amount) {
        ContractSummaryResponse contract = new ContractSummaryResponse();
        contract.setId(contractId);
        contract.setCompanyId(companyId);
        contract.setProfessionalId(professionalId);
        contract.setAmount(amount);
        contract.setStatus("COMPLETED");
        return contract;
    }

    @Test
    void completedContract_initiatePayment_calculatesCommissionAndPublishesEvent() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
        when(contractServiceClient.getContract(contractId))
                .thenReturn(completedContract(contractId, companyId, professionalId, BigDecimal.valueOf(1000)));

        String response = mockMvc.perform(post("/api/v1/payments")
                        .with(jwt().authorities(COMPANY))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestFor(contractId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(1000))
                .andExpect(jsonPath("$.commissionAmount").value(80.00))
                .andExpect(jsonPath("$.netAmount").value(920.00))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn().getResponse().getContentAsString();
        UUID transactionId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

        assertThat(receiveEventType()).isEqualTo("payment.completed");

        mockMvc.perform(get("/api/v1/transactions/{id}/invoice", transactionId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commission").value(80.00))
                .andExpect(jsonPath("$.professionalFee").value(920.00))
                .andExpect(jsonPath("$.invoiceNumber").exists());
    }

    @Test
    void initiatePayment_rejectsDuplicatePaymentForSameContract() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
        when(contractServiceClient.getContract(contractId))
                .thenReturn(completedContract(contractId, companyId, professionalId, BigDecimal.valueOf(500)));

        mockMvc.perform(post("/api/v1/payments")
                        .with(jwt().authorities(COMPANY))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestFor(contractId))))
                .andExpect(status().isCreated());
        receiveEventType(); // drain payment.completed

        mockMvc.perform(post("/api/v1/payments")
                        .with(jwt().authorities(COMPANY))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestFor(contractId))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void initiatePayment_rejectsWhenContractNotCompleted() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        ContractSummaryResponse activeContract = completedContract(contractId, companyId, professionalId, BigDecimal.valueOf(500));
        activeContract.setStatus("ACTIVE");

        when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
        when(contractServiceClient.getContract(contractId)).thenReturn(activeContract);

        mockMvc.perform(post("/api/v1/payments")
                        .with(jwt().authorities(COMPANY))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestFor(contractId))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void adminUpdatesCommissionRate_futurePaymentsUseNewRate() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        when(userServiceClient.resolveCurrentUserId()).thenReturn(adminId);
        CommissionConfigRequest rateRequest = new CommissionConfigRequest();
        rateRequest.setRatePercentage(BigDecimal.valueOf(15));
        mockMvc.perform(put("/api/v1/admin/commission-config")
                        .with(jwt().authorities(ADMIN))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(rateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratePercentage").value(15.00));

        when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
        when(contractServiceClient.getContract(contractId))
                .thenReturn(completedContract(contractId, companyId, professionalId, BigDecimal.valueOf(1000)));

        mockMvc.perform(post("/api/v1/payments")
                        .with(jwt().authorities(COMPANY))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestFor(contractId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commissionAmount").value(150.00));
        receiveEventType();
    }

    private InitiatePaymentRequest requestFor(UUID contractId) {
        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setContractId(contractId);
        return request;
    }
}
