package com.jonathan.ride_matching_service.exception;

public record ErrorResponse(
        int status,
        String message
) {}

