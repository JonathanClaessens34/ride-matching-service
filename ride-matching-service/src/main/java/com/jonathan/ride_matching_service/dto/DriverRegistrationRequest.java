package com.jonathan.ride_matching_service.dto;

import jakarta.validation.constraints.NotNull;

public record DriverRegistrationRequest(
        @NotNull String driverId,
        @NotNull double x,
        @NotNull double y
) {}
