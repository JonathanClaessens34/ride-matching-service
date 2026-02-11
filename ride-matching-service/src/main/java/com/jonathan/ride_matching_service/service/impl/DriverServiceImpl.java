package com.jonathan.ride_matching_service.service.impl;

import com.jonathan.ride_matching_service.dto.DriverResponse;
import com.jonathan.ride_matching_service.exception.NotFoundException;
import com.jonathan.ride_matching_service.exception.RepositorySaveException;
import com.jonathan.ride_matching_service.mapper.DriverMapper;
import com.jonathan.ride_matching_service.model.Driver;
import com.jonathan.ride_matching_service.model.Location;
import com.jonathan.ride_matching_service.repository.DriverRepository;
import com.jonathan.ride_matching_service.service.DriverService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final DriverMapper driverMapper;

    public DriverServiceImpl(DriverRepository driverRepository, DriverMapper driverMapper) {
        this.driverRepository = driverRepository;
        this.driverMapper = driverMapper;
    }

    @Override
    public void registerDriver(String driverId, Location location) {
        try {
            Driver driver = new Driver(driverId, location);
            driverRepository.save(driver);
        } catch (Exception e) {
            throw new RepositorySaveException("Failed to register driver: " + driverId);
        }
    }

    @Override
    public List<Driver> getAvailableDrivers() {
        return driverRepository.findAll()
                .stream()
                .filter(Driver::isAvailable)
                .toList();
    }

    @Override
    public DriverResponse updateDriver(String driverId, Location location, boolean available) {
        Driver driver = driverRepository.findById(driverId);

        if (driver == null) {
            throw new NotFoundException("Driver not found with id: " + driverId);
        }

        driver.updateLocation(location);

        if (available) {
            driver.release();
        } else {
            driver.tryMarkUnavailable();
        }

        return driverMapper.toDriverResponse(driver);
    }
}
