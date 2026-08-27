package com.skillmatch.contractservice.controller;

import com.skillmatch.contractservice.client.UserServiceClient;
import com.skillmatch.contractservice.dto.response.ContractResponse;
import com.skillmatch.contractservice.service.ContractService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
@Tag(name = "Contracts", description = "Micro-contract signature and lifecycle management")
@SecurityRequirement(name = "bearerAuth")
public class ContractController {

    private final ContractService contractService;
    private final UserServiceClient userServiceClient;

    @Operation(
            summary = "Get contract detail",
            description = "Returns a single micro-contract."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contract returned",
                    content = @Content(schema = @Schema(implementation = ContractResponse.class))),
            @ApiResponse(responseCode = "404", description = "Contract not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{contractId}")
    public ResponseEntity<ContractResponse> getContract(
            @Parameter(description = "Contract UUID", required = true)
            @PathVariable UUID contractId) {
        return ResponseEntity.ok(contractService.getContract(contractId));
    }

    @Operation(
            summary = "List my contracts as company",
            description = "Returns all contracts where the authenticated caller is the company party."
    )
    @GetMapping("/company/mine")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<List<ContractResponse>> listMyContractsAsCompany() {
        UUID companyId = userServiceClient.resolveCurrentUserId();
        return ResponseEntity.ok(contractService.listContractsByCompany(companyId));
    }

    @Operation(
            summary = "List my contracts as professional",
            description = "Returns all contracts where the authenticated caller is the professional party."
    )
    @GetMapping("/professional/mine")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<List<ContractResponse>> listMyContractsAsProfessional() {
        UUID professionalId = userServiceClient.resolveCurrentUserId();
        return ResponseEntity.ok(contractService.listContractsByProfessional(professionalId));
    }

    @Operation(
            summary = "Sign a contract",
            description = "Signs the contract on behalf of the caller. The company party must sign first "
                    + "(DRAFT -> PENDING_SIGNATURES), then the professional party (PENDING_SIGNATURES -> ACTIVE). (UC-C2)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contract signed",
                    content = @Content(schema = @Schema(implementation = ContractResponse.class))),
            @ApiResponse(responseCode = "404", description = "Contract not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Caller is not the expected party for the current "
                    + "stage, or the contract is not awaiting a signature",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{contractId}/sign")
    @PreAuthorize("hasAnyRole('COMPANY', 'PROFESSIONAL')")
    public ResponseEntity<ContractResponse> signContract(
            @Parameter(description = "Contract UUID", required = true)
            @PathVariable UUID contractId) {
        UUID callerId = userServiceClient.resolveCurrentUserId();
        return ResponseEntity.ok(contractService.signContract(callerId, contractId));
    }

    @Operation(
            summary = "Complete a contract",
            description = "Marks an ACTIVE contract as COMPLETED. Only the owning company may complete it."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contract completed",
                    content = @Content(schema = @Schema(implementation = ContractResponse.class))),
            @ApiResponse(responseCode = "404", description = "Contract not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Caller is not the owning company, or the contract "
                    + "is not ACTIVE",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{contractId}/complete")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<ContractResponse> completeContract(
            @Parameter(description = "Contract UUID", required = true)
            @PathVariable UUID contractId) {
        UUID companyId = userServiceClient.resolveCurrentUserId();
        return ResponseEntity.ok(contractService.completeContract(companyId, contractId));
    }
}
