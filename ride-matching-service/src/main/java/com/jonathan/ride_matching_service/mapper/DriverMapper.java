package com.jonathan.ride_matching_service.mapper;

import com.jonathan.ride_matching_service.dto.DriverResponse;
import com.jonathan.ride_matching_service.model.Driver;
import org.springframework.stereotype.Component;

@Component
public class DriverMapper {

    public DriverResponse toDriverResponse(Driver driver) {
        return new DriverResponse(
                driver.getLocation().x(),
                driver.getLocation().y(),
                driver.isAvailable()
        );
    }
}
