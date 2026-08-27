package com.skillmatch.notificationservice.controller;

import com.skillmatch.notificationservice.client.UserServiceClient;
import com.skillmatch.notificationservice.dto.response.NotificationResponse;
import com.skillmatch.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification retrieval")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserServiceClient userServiceClient;

    @Operation(
            summary = "List my notifications",
            description = "Returns all notifications addressed to the authenticated caller, most recent first."
    )
    @GetMapping("/mine")
    public ResponseEntity<List<NotificationResponse>> listMine() {
        UUID recipientId = userServiceClient.resolveCurrentUserId();
        return ResponseEntity.ok(notificationService.listMine(recipientId));
    }
}
