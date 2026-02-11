package com.jonathan.ride_matching_service.dto;

public record DriverResponse(
        String id,
        double x,
        double y,
        boolean available
) {}
