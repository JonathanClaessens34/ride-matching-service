package com.jonathan.ride_matching_service.service.impl;

import com.jonathan.ride_matching_service.dto.RideResponse;
import com.jonathan.ride_matching_service.exception.NotFoundException;
import com.jonathan.ride_matching_service.exception.RepositorySaveException;
import com.jonathan.ride_matching_service.mapper.RideMapper;
import com.jonathan.ride_matching_service.model.Driver;
import com.jonathan.ride_matching_service.model.Location;
import com.jonathan.ride_matching_service.model.Ride;
import com.jonathan.ride_matching_service.repository.RideRepository;
import com.jonathan.ride_matching_service.service.MatchingService;
import com.jonathan.ride_matching_service.service.RideService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RideServiceImpl implements RideService {

    private final MatchingService matchingService;
    private final RideRepository rideRepository;
    private final RideMapper rideMapper;

    public RideServiceImpl(MatchingService matchingService,
                           RideRepository rideRepository,
                           RideMapper rideMapper) {
        this.matchingService = matchingService;
        this.rideRepository = rideRepository;
        this.rideMapper = rideMapper;
    }

    @Override
    public RideResponse requestRide(String riderId, Location pickupLocation) {
        Driver driver = matchingService.findNearestAvailableDriver(pickupLocation);

        String rideId = UUID.randomUUID().toString();

        Ride ride = new Ride(
                rideId,
                riderId,
                driver,
                pickupLocation
        );

        try {
            rideRepository.save(ride);
            return rideMapper.toRideResponse(ride);
        } catch (Exception e) {
            driver.release();
            throw new RepositorySaveException("Failed to save ride: " + e.getMessage());
        }
    }

    @Override
    public void completeRide(String rideId) {
        Ride ride = rideRepository.findById(rideId);

        if (ride == null) {
            throw new NotFoundException("Ride not found");
        }

        if (ride.isCompleted()) {
            throw new RuntimeException("Ride already completed");
        }

        ride.complete();
        ride.getDriver().release();
    }
}
