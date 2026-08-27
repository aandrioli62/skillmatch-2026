package com.skillmatch.feedbackservice.controller;

import com.skillmatch.feedbackservice.client.UserServiceClient;
import com.skillmatch.feedbackservice.dto.request.SubmitFeedbackRequest;
import com.skillmatch.feedbackservice.dto.response.FeedbackResponse;
import com.skillmatch.feedbackservice.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/feedbacks")
@RequiredArgsConstructor
@Tag(name = "Feedbacks", description = "Mutual feedback submission and retrieval")
@SecurityRequirement(name = "bearerAuth")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final UserServiceClient userServiceClient;

    @Operation(
            summary = "Submit feedback",
            description = "Submits feedback from the caller for the other party on a project. The reviewee is "
                    + "derived from the project's eligibility (enabled by payment.completed), never taken from "
                    + "client input. Only the professional's reputation is recalculated from received feedback."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Feedback submitted",
                    content = @Content(schema = @Schema(implementation = FeedbackResponse.class))),
            @ApiResponse(responseCode = "422", description = "Project not yet eligible for feedback, caller is not "
                    + "a party to it, or has already reviewed the other party",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "User Service unavailable — caller identity could not be resolved",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY', 'PROFESSIONAL')")
    public ResponseEntity<FeedbackResponse> submitFeedback(@Valid @RequestBody SubmitFeedbackRequest request) {
        UUID callerId = userServiceClient.resolveCurrentUserId();
        FeedbackResponse response = feedbackService.submitFeedback(
                callerId, request.getProjectId(), request.getRating(), request.getComment());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get feedback detail",
            description = "Returns a single feedback."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedback returned",
                    content = @Content(schema = @Schema(implementation = FeedbackResponse.class))),
            @ApiResponse(responseCode = "404", description = "Feedback not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{feedbackId}")
    public ResponseEntity<FeedbackResponse> getFeedback(
            @Parameter(description = "Feedback UUID", required = true)
            @PathVariable UUID feedbackId) {
        return ResponseEntity.ok(feedbackService.getFeedback(feedbackId));
    }

    @Operation(
            summary = "List feedback for a project",
            description = "Returns both feedbacks (company -> professional and professional -> company) left on a project."
    )
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<FeedbackResponse>> listByProject(
            @Parameter(description = "Project UUID", required = true)
            @PathVariable UUID projectId) {
        return ResponseEntity.ok(feedbackService.listByProject(projectId));
    }

    @Operation(
            summary = "List feedback I have given",
            description = "Returns all feedback the authenticated caller has given to others."
    )
    @GetMapping("/given/mine")
    public ResponseEntity<List<FeedbackResponse>> listGivenByMe() {
        UUID callerId = userServiceClient.resolveCurrentUserId();
        return ResponseEntity.ok(feedbackService.listGivenByReviewer(callerId));
    }

    @Operation(
            summary = "List feedback I have received",
            description = "Returns all feedback the authenticated caller has received from others. (UC-P3)"
    )
    @GetMapping("/received/mine")
    public ResponseEntity<List<FeedbackResponse>> listReceivedByMe() {
        UUID callerId = userServiceClient.resolveCurrentUserId();
        return ResponseEntity.ok(feedbackService.listReceivedByReviewee(callerId));
    }
}
