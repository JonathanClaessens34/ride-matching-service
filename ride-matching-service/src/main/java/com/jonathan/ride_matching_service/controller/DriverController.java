package com.jonathan.ride_matching_service.controller;

import com.jonathan.ride_matching_service.dto.DriverRegistrationRequest;
import com.jonathan.ride_matching_service.dto.DriverResponse;
import com.jonathan.ride_matching_service.dto.UpdateDriverRequest;
import com.jonathan.ride_matching_service.model.Location;
import com.jonathan.ride_matching_service.service.DriverService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    public ResponseEntity<Void> registerDriver(@RequestBody DriverRegistrationRequest request) {
        driverService.registerDriver(
                request.driverId(),
                new Location(request.x(), request.y())
        );
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{driverId}")
    public ResponseEntity<DriverResponse> updateDriver(@PathVariable String driverId, @RequestBody UpdateDriverRequest request) {
        DriverResponse updatedDriver = driverService.updateDriver(
                driverId,
                new Location(request.x(), request.y()),
                request.available()
        );
        return ResponseEntity.ok(updatedDriver);
    }

}
