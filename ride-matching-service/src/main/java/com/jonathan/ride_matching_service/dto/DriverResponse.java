package com.jonathan.ride_matching_service.dto;

public record DriverResponse(
        double x,
        double y,
        boolean available
) {}
