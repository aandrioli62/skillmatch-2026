package com.skillmatch.paymentservice.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class CommissionConfigResponse {

    private UUID id;
    private BigDecimal ratePercentage;
    private LocalDateTime effectiveFrom;
    private UUID setByAdminId;
}
