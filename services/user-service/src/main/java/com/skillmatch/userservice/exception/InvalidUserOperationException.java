package com.skillmatch.userservice.exception;

public class InvalidUserOperationException extends RuntimeException {

    public InvalidUserOperationException(String message) {
        super(message);
    }
}
