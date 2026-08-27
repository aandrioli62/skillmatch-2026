package com.skillmatch.paymentservice.exception;

/**
 * Thrown when Contract Service cannot be reached (or its circuit breaker is open)
 * while resolving a contract's amount and status. Fails closed: the payment is
 * rejected rather than assuming the contract is payable.
 */
public class ContractServiceUnavailableException extends RuntimeException {

    public ContractServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
