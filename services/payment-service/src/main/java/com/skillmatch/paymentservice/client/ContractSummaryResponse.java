package com.skillmatch.paymentservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Minimal shadow of Contract Service's ContractResponse — only the fields Payment
 * Service needs to authorize and price a payment. Kept separate from Contract
 * Service's own DTOs to preserve Database-per-Service independence.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class ContractSummaryResponse {

    private UUID id;
    private UUID projectId;
    private UUID professionalId;
    private UUID companyId;
    private BigDecimal amount;
    private String status;
}
