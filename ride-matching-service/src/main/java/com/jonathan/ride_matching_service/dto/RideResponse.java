package com.jonathan.ride_matching_service.dto;

public record RideResponse(
        String rideId,
        String driverId,
        String riderId,
        double pickupX,
        double pickupY
) {}
