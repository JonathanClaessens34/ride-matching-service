package com.jonathan.ride_matching_service.service.impl;

import com.jonathan.ride_matching_service.dto.DriverResponse;
import com.jonathan.ride_matching_service.exception.NotFoundException;
import com.jonathan.ride_matching_service.mapper.DriverMapper;
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
    private final DriverMapper driverMapper;

    public MatchingServiceImpl(DriverService driverService, DriverMapper driverMapper) {
        this.driverService = driverService;
        this.driverMapper = driverMapper;
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

    @Override
    public List<DriverResponse> findNearestAvailableDrivers(Location pickupLocation, int limit) {
        List<Driver> drivers = driverService.getAvailableDrivers();

        return drivers.stream()
                .sorted(Comparator.comparingDouble(
                        d -> DistanceCalculator.distance(d.getLocation(), pickupLocation)
                ))
                .limit(limit)
                .map(driverMapper::toDriverResponse)
                .toList();
    }
}
