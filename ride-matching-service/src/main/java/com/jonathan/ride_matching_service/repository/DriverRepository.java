package com.jonathan.ride_matching_service.repository;

import com.jonathan.ride_matching_service.model.Driver;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class DriverRepository {

    private final ConcurrentHashMap<String, Driver> drivers = new ConcurrentHashMap<>();

    public void save(Driver driver) {
        drivers.put(driver.getId(), driver);
    }

    public Driver findById(String id) {
        return drivers.get(id);
    }

    public List<Driver> findAll() {
        return new ArrayList<>(drivers.values());
    }
}
