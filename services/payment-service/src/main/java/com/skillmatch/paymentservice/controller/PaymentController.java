package com.skillmatch.paymentservice.controller;

import com.skillmatch.paymentservice.client.UserServiceClient;
import com.skillmatch.paymentservice.dto.request.InitiatePaymentRequest;
import com.skillmatch.paymentservice.dto.response.TransactionResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment initiation with commission calculation")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserServiceClient userServiceClient;

    @Operation(
            summary = "Initiate payment for a completed contract",
            description = "Fetches the contract from Contract Service, computes the platform commission at the "
                    + "currently active rate, records the transaction, generates the invoice, and publishes "
                    + "payment.completed. Only the contract's company may pay it. (UC-C3)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment completed",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "422", description = "Caller is not the contract's company, the contract "
                    + "is not COMPLETED, or it has already been paid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "Contract Service unavailable — contract could not be verified",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<TransactionResponse> initiatePayment(@Valid @RequestBody InitiatePaymentRequest request) {
        UUID companyId = userServiceClient.resolveCurrentUserId();
        TransactionResponse response = paymentService.initiatePayment(companyId, request.getContractId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
