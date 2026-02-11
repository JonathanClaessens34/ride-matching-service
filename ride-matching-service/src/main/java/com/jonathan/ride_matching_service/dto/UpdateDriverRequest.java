package com.jonathan.ride_matching_service.dto;

public record UpdateDriverRequest(
        double x,
        double y,
        boolean available
) {}
