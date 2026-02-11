package com.jonathan.ride_matching_service.dto;

public record RideRequest(
        String riderId,
        double x,
        double y
) {}
