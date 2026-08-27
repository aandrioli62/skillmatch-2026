package com.skillmatch.paymentservice.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CommissionConfigRequest {

    @NotNull(message = "Rate percentage is required")
    @DecimalMin(value = "0.0", message = "Rate percentage must not be negative")
    @DecimalMax(value = "100.0", message = "Rate percentage must not exceed 100")
    private BigDecimal ratePercentage;
}
