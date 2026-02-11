package com.jonathan.ride_matching_service.controller;

import com.jonathan.ride_matching_service.dto.RideRequest;
import com.jonathan.ride_matching_service.dto.RideResponse;
import com.jonathan.ride_matching_service.model.Location;
import com.jonathan.ride_matching_service.service.RideService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping
    public RideResponse requestRide(@RequestBody RideRequest request) {
        return rideService.requestRide(
                request.riderId(),
                new Location(request.x(), request.y())
        );
    }

    @PostMapping("/{rideId}/complete")
    public void completeRide(@PathVariable String rideId) {
        rideService.completeRide(rideId);
    }
}
