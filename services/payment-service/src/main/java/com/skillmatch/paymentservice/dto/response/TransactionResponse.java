package com.skillmatch.paymentservice.dto.response;

import com.skillmatch.paymentservice.model.enums.TransactionStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class TransactionResponse {

    private UUID id;
    private UUID contractId;
    private UUID companyId;
    private UUID professionalId;
    private BigDecimal totalAmount;
    private BigDecimal commissionAmount;
    private BigDecimal netAmount;
    private TransactionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
