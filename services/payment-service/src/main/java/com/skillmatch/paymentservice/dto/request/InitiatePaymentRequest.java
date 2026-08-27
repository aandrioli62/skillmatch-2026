package com.skillmatch.paymentservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class InitiatePaymentRequest {

    @NotNull(message = "Contract id is required")
    private UUID contractId;
}
