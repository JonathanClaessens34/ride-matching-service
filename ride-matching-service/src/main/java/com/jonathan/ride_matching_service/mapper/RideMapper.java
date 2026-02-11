package com.jonathan.ride_matching_service.mapper;

import com.jonathan.ride_matching_service.dto.RideResponse;
import com.jonathan.ride_matching_service.model.Ride;
import org.springframework.stereotype.Component;

@Component
public class RideMapper {

    public RideResponse toRideResponse(Ride ride) {
        return new RideResponse(
                ride.getId(),
                ride.getDriver().getId(),
                ride.getRiderId(),
                ride.getPickupLocation().x(),
                ride.getPickupLocation().y()
        );
    }
}

