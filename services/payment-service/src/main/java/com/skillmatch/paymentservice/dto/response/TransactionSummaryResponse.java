package com.skillmatch.paymentservice.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TransactionSummaryResponse {

    private BigDecimal totalVolume;
    private BigDecimal totalCommission;
    private BigDecimal totalNet;
    private long count;
}
