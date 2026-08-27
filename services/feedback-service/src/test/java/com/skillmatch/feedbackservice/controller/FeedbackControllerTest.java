package com.skillmatch.feedbackservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatch.feedbackservice.client.UserServiceClient;
import com.skillmatch.feedbackservice.config.TestSecurityConfig;
import com.skillmatch.feedbackservice.dto.request.SubmitFeedbackRequest;
import com.skillmatch.feedbackservice.dto.response.FeedbackResponse;
import com.skillmatch.feedbackservice.exception.FeedbackNotFoundException;
import com.skillmatch.feedbackservice.exception.GlobalExceptionHandler;
import com.skillmatch.feedbackservice.exception.InvalidFeedbackOperationException;
import com.skillmatch.feedbackservice.service.FeedbackService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedbackController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("FeedbackController — @WebMvcTest")
class FeedbackControllerTest {

    private static final SimpleGrantedAuthority ROLE_COMPANY = new SimpleGrantedAuthority("ROLE_COMPANY");
    private static final SimpleGrantedAuthority ROLE_PROFESSIONAL = new SimpleGrantedAuthority("ROLE_PROFESSIONAL");
    private static final SimpleGrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FeedbackService feedbackService;

    @MockBean
    private UserServiceClient userServiceClient;

    private SubmitFeedbackRequest buildRequest(UUID projectId) {
        SubmitFeedbackRequest request = new SubmitFeedbackRequest();
        request.setProjectId(projectId);
        request.setRating(5);
        request.setComment("Ottimo lavoro");
        return request;
    }

    @Nested
    @DisplayName("POST /api/v1/feedbacks")
    class SubmitFeedback {

        @Test
        @DisplayName("COMPANY caller → 201 Created")
        void submitFeedback_companySuccess() throws Exception {
            UUID companyId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            FeedbackResponse response = new FeedbackResponse();
            response.setRating(5);

            when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
            when(feedbackService.submitFeedback(eq(companyId), eq(projectId), eq(5), any()))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/feedbacks")
                            .with(jwt().authorities(ROLE_COMPANY))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest(projectId))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.rating").value(5));
        }

        @Test
        @DisplayName("PROFESSIONAL caller → 201 Created")
        void submitFeedback_professionalSuccess() throws Exception {
            UUID professionalId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            when(userServiceClient.resolveCurrentUserId()).thenReturn(professionalId);
            when(feedbackService.submitFeedback(eq(professionalId), eq(projectId), eq(5), any()))
                    .thenReturn(new FeedbackResponse());

            mockMvc.perform(post("/api/v1/feedbacks")
                            .with(jwt().authorities(ROLE_PROFESSIONAL))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest(projectId))))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("ADMIN role → 403 Forbidden")
        void submitFeedback_adminRole_forbidden() throws Exception {
            mockMvc.perform(post("/api/v1/feedbacks")
                            .with(jwt().authorities(ROLE_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest(UUID.randomUUID()))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("no token → 401 Unauthorized")
        void submitFeedback_noToken_unauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/feedbacks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest(UUID.randomUUID()))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("rating out of range → 400 Bad Request")
        void submitFeedback_invalidRating_badRequest() throws Exception {
            SubmitFeedbackRequest request = buildRequest(UUID.randomUUID());
            request.setRating(6);

            mockMvc.perform(post("/api/v1/feedbacks")
                            .with(jwt().authorities(ROLE_COMPANY))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("project not eligible → 422 Unprocessable Entity")
        void submitFeedback_notEligible_unprocessable() throws Exception {
            UUID companyId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            when(userServiceClient.resolveCurrentUserId()).thenReturn(companyId);
            when(feedbackService.submitFeedback(eq(companyId), eq(projectId), eq(5), any()))
                    .thenThrow(new InvalidFeedbackOperationException("not yet eligible"));

            mockMvc.perform(post("/api/v1/feedbacks")
                            .with(jwt().authorities(ROLE_COMPANY))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest(projectId))))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/feedbacks/{feedbackId}")
    class GetFeedback {

        @Test
        @DisplayName("existing feedback → 200 OK")
        void getFeedback_found() throws Exception {
            UUID feedbackId = UUID.randomUUID();
            FeedbackResponse response = new FeedbackResponse();
            response.setId(feedbackId);
            when(feedbackService.getFeedback(feedbackId)).thenReturn(response);

            mockMvc.perform(get("/api/v1/feedbacks/{feedbackId}", feedbackId).with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(feedbackId.toString()));
        }

        @Test
        @DisplayName("unknown feedback → 404 Not Found")
        void getFeedback_notFound() throws Exception {
            UUID feedbackId = UUID.randomUUID();
            when(feedbackService.getFeedback(feedbackId)).thenThrow(new FeedbackNotFoundException(feedbackId));

            mockMvc.perform(get("/api/v1/feedbacks/{feedbackId}", feedbackId).with(jwt()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/feedbacks/received/mine")
    class ListReceived {

        @Test
        @DisplayName("PROFESSIONAL caller → 200 OK with own received feedback")
        void listReceivedByMe_success() throws Exception {
            UUID professionalId = UUID.randomUUID();
            when(userServiceClient.resolveCurrentUserId()).thenReturn(professionalId);
            when(feedbackService.listReceivedByReviewee(professionalId)).thenReturn(List.of(new FeedbackResponse()));

            mockMvc.perform(get("/api/v1/feedbacks/received/mine").with(jwt().authorities(ROLE_PROFESSIONAL)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }
}
