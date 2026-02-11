package com.jonathan.ride_matching_service.model;

import lombok.Getter;

public class Ride {
    @Getter
    private final String id;
    @Getter
    private final String riderId;
    @Getter
    private final Driver driver;
    @Getter
    private final Location pickupLocation;
    @Getter
    private boolean completed;

    public Ride(String id, String riderId, Driver driver, Location pickupLocation) {
        this.id = id;
        this.riderId = riderId;
        this.driver = driver;
        this.pickupLocation = pickupLocation;
    }

    public void complete() {
        this.completed = true;
    }
}
