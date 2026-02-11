package com.jonathan.ride_matching_service.model;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;

public class Driver {
    @Getter
    private String id;
    @Getter
    private volatile Location location;
    private final AtomicBoolean available = new AtomicBoolean(true);

    public Driver(String  id, Location location) {
        this.location = location;
        this.id = id;
    }

    public void updateLocation(Location location) {
        this.location = location;
    }

    public boolean isAvailable() {
        return available.get();
    }

    /**
     * Atomically marks the driver as unavailable.
     * @return true if successfully marked unavailable, false otherwise
     */
    public boolean tryMarkUnavailable() {
        return available.compareAndSet(true, false);
    }

    public void release() {
        available.set(true);
    }
}
