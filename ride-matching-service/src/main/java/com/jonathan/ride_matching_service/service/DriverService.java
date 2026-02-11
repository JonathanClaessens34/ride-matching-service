package com.jonathan.ride_matching_service.service;

import com.jonathan.ride_matching_service.dto.DriverResponse;
import com.jonathan.ride_matching_service.model.Driver;
import com.jonathan.ride_matching_service.model.Location;

import java.util.List;

public interface DriverService {

    void registerDriver(String driverId, Location location);

    List<Driver> getAvailableDrivers();

    DriverResponse updateDriver(String driverId, Location location, boolean available);
}
