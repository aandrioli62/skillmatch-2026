package com.skillmatch.paymentservice.controller;

import com.skillmatch.paymentservice.dto.response.CommissionConfigResponse;
import com.skillmatch.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/commission-config")
@RequiredArgsConstructor
@Tag(name = "Commission Config", description = "Read-only access to the current platform commission rate")
@SecurityRequirement(name = "bearerAuth")
public class CommissionConfigController {

    private final PaymentService paymentService;

    @Operation(
            summary = "Get the current commission rate",
            description = "Returns the platform commission rate currently applied to new payments. Open to any "
                    + "authenticated caller (unlike /admin/commission-config) — companies and professionals need "
                    + "it to show an accurate estimate on a contract before it is paid, since the actual charge "
                    + "always uses whatever rate is current at payment time."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Commission config returned",
                    content = @Content(schema = @Schema(implementation = CommissionConfigResponse.class)))
    })
    @GetMapping("/current")
    public ResponseEntity<CommissionConfigResponse> getCurrentCommissionConfig() {
        return ResponseEntity.ok(paymentService.getCurrentCommissionConfig());
    }
}
