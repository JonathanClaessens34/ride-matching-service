package com.jonathan.ride_matching_service.controller;

import com.jonathan.ride_matching_service.dto.DriverResponse;
import com.jonathan.ride_matching_service.dto.RideRequest;
import com.jonathan.ride_matching_service.dto.RideResponse;
import com.jonathan.ride_matching_service.mapper.DriverMapper;
import com.jonathan.ride_matching_service.model.Location;
import com.jonathan.ride_matching_service.service.MatchingService;
import com.jonathan.ride_matching_service.service.RideService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rides")
public class RideController {

    private final RideService rideService;
    private final MatchingService matchingService;

    public RideController(RideService rideService, MatchingService matchingService) {
        this.rideService = rideService;
        this.matchingService = matchingService;
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

    @GetMapping("/available-drivers")
    public List<DriverResponse> getAvailableDrivers(@RequestParam double x, @RequestParam double y, @RequestParam(defaultValue = "5") int limit) {
        return matchingService.findNearestAvailableDrivers(
                new Location(x, y),
                limit
        );
    }
}
