package com.jonathan.ride_matching_service.util;

import com.jonathan.ride_matching_service.model.Location;

public class DistanceCalculator {

    public static double distance(Location a, Location b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
