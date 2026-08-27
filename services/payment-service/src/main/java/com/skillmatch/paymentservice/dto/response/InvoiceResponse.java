package com.skillmatch.paymentservice.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class InvoiceResponse {

    private UUID id;
    private UUID transactionId;
    private String invoiceNumber;
    private UUID companyId;
    private BigDecimal total;
    private BigDecimal commission;
    private BigDecimal professionalFee;
    private String pdfUrl;
    private LocalDateTime issuedAt;
}
