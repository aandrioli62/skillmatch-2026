package com.skillmatch.contractservice.dto.response;

import com.skillmatch.contractservice.model.enums.ContractStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class ContractResponse {

    private UUID id;
    private UUID projectId;
    private UUID professionalId;
    private UUID companyId;
    private BigDecimal amount;
    private BigDecimal commissionRate;
    private ContractStatus status;
    private String terms;
    private LocalDateTime createdAt;
    private LocalDateTime signedAt;
}
