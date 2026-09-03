package com.skillmatch.paymentservice.controller;

import com.skillmatch.paymentservice.client.UserServiceClient;
import com.skillmatch.paymentservice.dto.response.InvoiceResponse;
import com.skillmatch.paymentservice.dto.response.TransactionResponse;
import com.skillmatch.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction and invoice retrieval")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final PaymentService paymentService;
    private final UserServiceClient userServiceClient;

    @Operation(
            summary = "Get transaction detail",
            description = "Returns a single transaction. Only a party to the transaction (its company or its "
                    + "professional) or an admin may view it."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction returned",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not a party to the transaction",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @Parameter(description = "Transaction UUID", required = true)
            @PathVariable UUID transactionId,
            Authentication authentication) {
        UUID callerId = userServiceClient.resolveCurrentUserId();
        return ResponseEntity.ok(paymentService.getTransaction(transactionId, callerId, isAdmin(authentication)));
    }

    @Operation(
            summary = "Get the invoice for a transaction",
            description = "Returns the invoice generated when the transaction was paid. Only a party to the "
                    + "underlying transaction (its company or its professional) or an admin may view it."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invoice returned",
                    content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not a party to the underlying transaction",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Invoice not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{transactionId}/invoice")
    public ResponseEntity<InvoiceResponse> getInvoice(
            @Parameter(description = "Transaction UUID", required = true)
            @PathVariable UUID transactionId,
            Authentication authentication) {
        UUID callerId = userServiceClient.resolveCurrentUserId();
        return ResponseEntity.ok(paymentService.getInvoiceByTransaction(transactionId, callerId, isAdmin(authentication)));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    @Operation(
            summary = "List my transactions as company",
            description = "Returns all transactions where the authenticated caller is the paying company."
    )
    @GetMapping("/company/mine")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<List<TransactionResponse>> listMyTransactionsAsCompany() {
        UUID companyId = userServiceClient.resolveCurrentUserId();
        return ResponseEntity.ok(paymentService.listTransactionsByCompany(companyId));
    }

    @Operation(
            summary = "List my transactions as professional",
            description = "Returns all transactions where the authenticated caller is the paid professional. (UC-P3)"
    )
    @GetMapping("/professional/mine")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<List<TransactionResponse>> listMyTransactionsAsProfessional() {
        UUID professionalId = userServiceClient.resolveCurrentUserId();
        return ResponseEntity.ok(paymentService.listTransactionsByProfessional(professionalId));
    }
}
