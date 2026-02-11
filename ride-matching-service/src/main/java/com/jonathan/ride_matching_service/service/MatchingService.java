package com.jonathan.ride_matching_service.service;

import com.jonathan.ride_matching_service.model.Driver;
import com.jonathan.ride_matching_service.model.Location;

public interface MatchingService {

    Driver findNearestAvailableDriver(Location pickupLocation);
}
