package com.jonathan.ride_matching_service.repository;

import com.jonathan.ride_matching_service.model.Ride;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class RideRepository {

    private final ConcurrentHashMap<String, Ride> rides = new ConcurrentHashMap<>();

    public void save(Ride ride) {
        rides.put(ride.getId(), ride);
    }

    public Ride findById(String id) {
        return rides.get(id);
    }

    public Collection<Ride> findAll() {
        return rides.values();
    }
}
