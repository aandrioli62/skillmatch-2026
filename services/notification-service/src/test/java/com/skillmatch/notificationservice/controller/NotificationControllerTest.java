package com.skillmatch.notificationservice.controller;

import com.skillmatch.notificationservice.client.UserServiceClient;
import com.skillmatch.notificationservice.config.TestSecurityConfig;
import com.skillmatch.notificationservice.dto.response.NotificationResponse;
import com.skillmatch.notificationservice.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(TestSecurityConfig.class)
@DisplayName("NotificationController — @WebMvcTest")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private UserServiceClient userServiceClient;

    @Test
    @DisplayName("authenticated caller → 200 OK with own notifications")
    void listMine_success() throws Exception {
        UUID recipientId = UUID.randomUUID();
        NotificationResponse notification = new NotificationResponse();
        notification.setMessage("Benvenuto su SkillMatch!");

        when(userServiceClient.resolveCurrentUserId()).thenReturn(recipientId);
        when(notificationService.listMine(recipientId)).thenReturn(List.of(notification));

        mockMvc.perform(get("/api/v1/notifications/mine").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].message").value("Benvenuto su SkillMatch!"));
    }

    @Test
    @DisplayName("no token → 401 Unauthorized")
    void listMine_noToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/mine"))
                .andExpect(status().isUnauthorized());
    }
}
