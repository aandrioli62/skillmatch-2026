package com.skillmatch.paymentservice.exception;

import java.util.UUID;

public class InvoiceNotFoundException extends RuntimeException {

    public InvoiceNotFoundException(UUID transactionId) {
        super("Invoice not found for transaction id: " + transactionId);
    }
}
