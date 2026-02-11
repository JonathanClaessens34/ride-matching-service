package com.jonathan.ride_matching_service.exception;

public class RepositorySaveException extends RuntimeException {
    public RepositorySaveException(String message) {
        super(message);
    }

    public RepositorySaveException(String message, Throwable cause) {
        super(message, cause);
    }
}

