package com.skillmatch.contractservice.exception;

public class InvalidContractOperationException extends RuntimeException {

    public InvalidContractOperationException(String message) {
        super(message);
    }
}
