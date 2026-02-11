package com.jonathan.ride_matching_service.dto;

public record DriverRegistrationRequest(
        String driverId,
        double x,
        double y
) {}
