package com.skillmatch.paymentservice.controller;

import com.skillmatch.paymentservice.client.UserServiceClient;
import com.skillmatch.paymentservice.dto.request.CommissionConfigRequest;
import com.skillmatch.paymentservice.dto.response.CommissionConfigResponse;
import com.skillmatch.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/commission-config")
@RequiredArgsConstructor
@Tag(name = "Admin - Commission Config", description = "Platform commission rate configuration")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommissionController {

    private final PaymentService paymentService;
    private final UserServiceClient userServiceClient;

    @Operation(
            summary = "Get the current commission rate",
            description = "Returns the platform commission rate currently applied to new payments. (UC-A2)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Commission config returned",
                    content = @Content(schema = @Schema(implementation = CommissionConfigResponse.class)))
    })
    @GetMapping
    public ResponseEntity<CommissionConfigResponse> getCurrentCommissionConfig() {
        return ResponseEntity.ok(paymentService.getCurrentCommissionConfig());
    }

    @Operation(
            summary = "Update the commission rate",
            description = "Records a new commission rate, effective immediately. Past transactions are unaffected "
                    + "— only future payments use the new rate. (UC-A2)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Commission rate updated",
                    content = @Content(schema = @Schema(implementation = CommissionConfigResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid rate percentage",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping
    public ResponseEntity<CommissionConfigResponse> updateCommissionRate(
            @Valid @RequestBody CommissionConfigRequest request) {
        UUID adminId = userServiceClient.resolveCurrentUserId();
        return ResponseEntity.ok(paymentService.updateCommissionRate(adminId, request));
    }
}
