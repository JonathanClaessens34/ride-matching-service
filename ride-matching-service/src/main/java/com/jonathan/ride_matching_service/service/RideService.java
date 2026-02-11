package com.jonathan.ride_matching_service.service;

import com.jonathan.ride_matching_service.dto.RideResponse;
import com.jonathan.ride_matching_service.model.Location;

public interface RideService {

    RideResponse requestRide(String riderId, Location pickupLocation);

    void completeRide(String rideId);
}
