package com.skillmatch.contractservice.exception;

import java.util.UUID;

public class ContractNotFoundException extends RuntimeException {

    public ContractNotFoundException(UUID contractId) {
        super("Contract not found with id: " + contractId);
    }
}
