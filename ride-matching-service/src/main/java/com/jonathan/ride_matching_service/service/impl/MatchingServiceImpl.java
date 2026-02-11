package com.jonathan.ride_matching_service.service.impl;

import com.jonathan.ride_matching_service.exception.NotFoundException;
import com.jonathan.ride_matching_service.model.Driver;
import com.jonathan.ride_matching_service.model.Location;
import com.jonathan.ride_matching_service.service.DriverService;
import com.jonathan.ride_matching_service.service.MatchingService;
import com.jonathan.ride_matching_service.util.DistanceCalculator;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class MatchingServiceImpl implements MatchingService {

    private final DriverService driverService;

    public MatchingServiceImpl(DriverService driverService) {
        this.driverService = driverService;
    }

    @Override
    public Driver findNearestAvailableDriver(Location pickupLocation) {
        List<Driver> drivers = driverService.getAvailableDrivers();

        return drivers.stream()
                .sorted(Comparator.comparingDouble(
                        d -> DistanceCalculator.distance(d.getLocation(), pickupLocation)
                ))
                .filter(Driver::tryMarkUnavailable)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No available drivers found"));
    }
}
