package com.jonathan.ride_matching_service.concurrency;

import com.jonathan.ride_matching_service.dto.RideResponse;
import com.jonathan.ride_matching_service.exception.NotFoundException;
import com.jonathan.ride_matching_service.mapper.DriverMapper;
import com.jonathan.ride_matching_service.mapper.RideMapper;
import com.jonathan.ride_matching_service.model.Driver;
import com.jonathan.ride_matching_service.model.Location;
import com.jonathan.ride_matching_service.repository.DriverRepository;
import com.jonathan.ride_matching_service.repository.RideRepository;
import com.jonathan.ride_matching_service.service.DriverService;
import com.jonathan.ride_matching_service.service.MatchingService;
import com.jonathan.ride_matching_service.service.RideService;
import com.jonathan.ride_matching_service.service.impl.DriverServiceImpl;
import com.jonathan.ride_matching_service.service.impl.MatchingServiceImpl;
import com.jonathan.ride_matching_service.service.impl.RideServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Ride Concurrency Tests")
class RideConcurrencyTest {

    private DriverRepository driverRepository;
    private RideRepository rideRepository;
    private DriverService driverService;
    private MatchingService matchingService;
    private RideService rideService;
    private DriverMapper driverMapper;
    private RideMapper rideMapper;

    @BeforeEach
    void setUp() {
        // Using real repositories to test concurrency behavior
        driverRepository = new DriverRepository();
        rideRepository = new RideRepository();
        driverMapper = new DriverMapper();
        rideMapper = new RideMapper();
        driverService = new DriverServiceImpl(driverRepository, driverMapper);
        matchingService = new MatchingServiceImpl(driverService, driverMapper);
        rideService = new RideServiceImpl(matchingService, rideRepository, rideMapper);
    }

    @Test
    @DisplayName("Two concurrent ride requests with one driver - only one succeeds")
    void testTwoConcurrentRideRequestsOneDriver() throws InterruptedException, java.util.concurrent.ExecutionException {
        // Given - Setup with 1 driver
        String driverId = "driver-concurrent-1";
        Location driverLocation = new Location(0, 0);
        driverService.registerDriver(driverId, driverLocation);

        Location pickupLocation = new Location(1, 1);
        String riderId1 = "rider-1";
        String riderId2 = "rider-2";

        // Setup concurrent execution
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(2);
        List<RideResponse> successfulRides = new ArrayList<>();
        List<Exception> failures = new ArrayList<>();

        try {
            // When - Two threads try to request rides simultaneously
            Future<Void> future1 = executor.submit(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await(); // Ensure both threads start at the same time
                    RideResponse response = rideService.requestRide(riderId1, pickupLocation);
                    successfulRides.add(response);
                } catch (Exception e) {
                    failures.add(e);
                }
                return null;
            });

            Future<Void> future2 = executor.submit(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await(); // Ensure both threads start at the same time
                    RideResponse response = rideService.requestRide(riderId2, pickupLocation);
                    successfulRides.add(response);
                } catch (Exception e) {
                    failures.add(e);
                }
                return null;
            });

            // Wait for both tasks to complete
            future1.get();
            future2.get();

            // Then - Verify only one ride succeeds
            assertThat(successfulRides).as("Only one ride should succeed").hasSize(1);
            assertThat(failures).as("One request should fail").hasSize(1);
            assertThat(failures.get(0)).isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("No available drivers found");

            // Verify only one ride is stored
            assertThat(rideRepository.findAll()).hasSize(1);

            // Verify the successful ride has the correct driver
            RideResponse successfulRide = successfulRides.get(0);
            assertThat(successfulRide.driverId()).isEqualTo(driverId);

            // Verify driver is unavailable (assigned to the ride)
            Driver driver = driverRepository.findById(driverId);
            assertThat(driver.isAvailable()).isFalse();

        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Multiple concurrent ride requests with multiple drivers - each gets one")
    void testMultipleConcurrentRequestsMultipleDrivers() throws InterruptedException, java.util.concurrent.ExecutionException {
        // Given - Setup with 3 drivers
        Location[] driverLocations = {
                new Location(0, 0),
                new Location(2, 2),
                new Location(4, 4)
        };
        String[] driverIds = new String[3];
        for (int i = 0; i < 3; i++) {
            driverIds[i] = "driver-multi-" + i;
            driverService.registerDriver(driverIds[i], driverLocations[i]);
        }

        Location pickupLocation = new Location(1, 1);
        int numberOfRequests = 3;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfRequests);
        CountDownLatch startLatch = new CountDownLatch(numberOfRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        try {
            // When - Multiple threads request rides simultaneously
            List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < numberOfRequests; i++) {
                final int index = i;
                futures.add(executor.submit(() -> {
                    try {
                        startLatch.countDown();
                        startLatch.await(); // Ensure all threads start at the same time
                        rideService.requestRide("rider-" + index, pickupLocation);
                        successCount.incrementAndGet();
                    } catch (NotFoundException e) {
                        failureCount.incrementAndGet();
                    }
                    return null;
                }));
            }

            // Wait for all tasks to complete
            for (Future<Void> future : futures) {
                future.get();
            }

            // Then - Verify results
            assertThat(successCount.get()).as("All 3 rides should succeed").isEqualTo(3);
            assertThat(failureCount.get()).as("No failures expected").isEqualTo(0);

            // Verify all drivers are now unavailable
            for (String driverId : driverIds) {
                Driver driver = driverRepository.findById(driverId);
                assertThat(driver.isAvailable()).isFalse();
            }

            // Verify all 3 rides are stored
            assertThat(rideRepository.findAll()).hasSize(3);

        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("High contention - many threads competing for limited drivers")
    void testHighContentionManyThreadsFewDrivers() throws InterruptedException, java.util.concurrent.ExecutionException {
        // Given - Setup with 2 drivers but 10 concurrent requests
        String driver1Id = "driver-limited-1";
        String driver2Id = "driver-limited-2";
        driverService.registerDriver(driver1Id, new Location(0, 0));
        driverService.registerDriver(driver2Id, new Location(1, 1));

        Location pickupLocation = new Location(2, 2);
        int numberOfRequests = 10;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfRequests);
        CountDownLatch startLatch = new CountDownLatch(numberOfRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        try {
            // When - 10 threads try to request rides simultaneously
            List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < numberOfRequests; i++) {
                final int index = i;
                futures.add(executor.submit(() -> {
                    try {
                        startLatch.countDown();
                        startLatch.await(); // Ensure all threads start at the same time
                        rideService.requestRide("rider-" + index, pickupLocation);
                        successCount.incrementAndGet();
                    } catch (NotFoundException e) {
                        failureCount.incrementAndGet();
                    }
                    return null;
                }));
            }

            // Wait for all tasks to complete
            for (Future<Void> future : futures) {
                future.get();
            }

            // Then - Verify exactly 2 rides succeed (one per driver)
            assertThat(successCount.get()).as("Exactly 2 rides should succeed").isEqualTo(2);
            assertThat(failureCount.get()).as("8 rides should fail").isEqualTo(8);

            // Verify both drivers are unavailable
            Driver driver1 = driverRepository.findById(driver1Id);
            Driver driver2 = driverRepository.findById(driver2Id);
            assertThat(driver1.isAvailable()).isFalse();
            assertThat(driver2.isAvailable()).isFalse();

            // Verify exactly 2 rides are stored
            assertThat(rideRepository.findAll()).hasSize(2);

        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Concurrent ride requests and completions - driver reusability")
    void testConcurrentRequestsAndCompletions() throws InterruptedException {
        // Given - Setup with 1 driver
        String driverId = "driver-reusable";
        driverService.registerDriver(driverId, new Location(0, 0));

        Location pickupLocation = new Location(1, 1);
        int numberOfRides = 5;

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch allRidesCreated = new CountDownLatch(numberOfRides);
        List<String> rideIds = new ArrayList<>();

        try {
            // When - Request 5 rides sequentially, completing each before the next
            for (int i = 0; i < numberOfRides; i++) {
                // Request a ride
                RideResponse response = rideService.requestRide("rider-" + i, pickupLocation);
                rideIds.add(response.rideId());
                allRidesCreated.countDown();

                // Verify driver is unavailable during ride
                Driver driver = driverRepository.findById(driverId);
                assertThat(driver.isAvailable()).isFalse();

                // Complete the ride
                rideService.completeRide(response.rideId());

                // Verify driver is available again
                driver = driverRepository.findById(driverId);
                assertThat(driver.isAvailable()).isTrue();
            }

            // Then - Verify all 5 rides were created and completed
            assertThat(rideIds).hasSize(numberOfRides);
            assertThat(rideRepository.findAll()).hasSize(numberOfRides);

            // Verify all rides are completed
            for (String rideId : rideIds) {
                assertThat(rideRepository.findById(rideId).isCompleted()).isTrue();
            }

            // Verify driver is available for new rides
            Driver driver = driverRepository.findById(driverId);
            assertThat(driver.isAvailable()).isTrue();

        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Atomic driver allocation - same driver cannot be assigned twice")
    void testAtomicDriverAllocation() throws InterruptedException, java.util.concurrent.ExecutionException {
        // Given - Setup with 1 driver and prepare for concurrent access
        String driverId = "driver-atomic";
        driverService.registerDriver(driverId, new Location(0, 0));

        Location pickupLocation = new Location(1, 1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(2);
        AtomicInteger rideCount = new AtomicInteger(0);

        try {
            // When - Two threads try to request rides at exactly the same time
            Future<Void> future1 = executor.submit(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await();
                    rideService.requestRide("rider-atomic-1", pickupLocation);
                    rideCount.incrementAndGet();
                } catch (NotFoundException e) {
                    // Expected - driver already allocated
                }
                return null;
            });

            Future<Void> future2 = executor.submit(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await();
                    rideService.requestRide("rider-atomic-2", pickupLocation);
                    rideCount.incrementAndGet();
                } catch (NotFoundException e) {
                    // Expected - driver already allocated
                }
                return null;
            });

            future1.get();
            future2.get();

            // Then - Verify driver was assigned to exactly one ride (atomicity preserved)
            assertThat(rideCount.get()).as("Only one ride should succeed").isEqualTo(1);

            // Verify the driver object wasn't modified by both threads
            Driver driver = driverRepository.findById(driverId);
            assertThat(driver.isAvailable()).isFalse();

            // Verify exactly one ride exists with this driver
            List<String> allRides = rideRepository.findAll().stream()
                    .map(ride -> ride.getId())
                    .toList();
            assertThat(allRides).hasSize(1);

        } finally {
            executor.shutdown();
        }
    }
}

