package com.jonathan.ride_matching_service.service;

import com.jonathan.ride_matching_service.dto.DriverResponse;
import com.jonathan.ride_matching_service.exception.NotFoundException;
import com.jonathan.ride_matching_service.mapper.DriverMapper;
import com.jonathan.ride_matching_service.model.Driver;
import com.jonathan.ride_matching_service.model.Location;
import com.jonathan.ride_matching_service.service.impl.MatchingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchingService Tests")
class MatchingServiceTest {

    @Mock
    private DriverService driverService;

    @Mock
    private DriverMapper driverMapper;

    private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        matchingService = new MatchingServiceImpl(driverService, driverMapper);
    }

    @Nested
    @DisplayName("Find Nearest Driver Tests")
    class FindNearestDriverTests {

        @Test
        @DisplayName("Should return nearest driver when 3 drivers at different distances")
        void testFindNearestDriverAmongMultiple() {
            // Given
            Location pickupLocation = new Location(0, 0);

            // Create 3 drivers at different distances
            // Driver 1: distance ≈ 5 (3, 4)
            Driver driver1 = new Driver("driver-1", new Location(3, 4));
            // Driver 2: distance ≈ 13 (5, 12)
            Driver driver2 = new Driver("driver-2", new Location(5, 12));
            // Driver 3: distance ≈ 1 (1, 0)
            Driver driver3 = new Driver("driver-3", new Location(1, 0));

            List<Driver> availableDrivers = Arrays.asList(driver1, driver2, driver3);

            when(driverService.getAvailableDrivers()).thenReturn(availableDrivers);

            // When
            Driver nearest = matchingService.findNearestAvailableDriver(pickupLocation);

            // Then
            assertThat(nearest).isEqualTo(driver3);
            assertThat(nearest.getId()).isEqualTo("driver-3");
        }

        @Test
        @DisplayName("Should mark selected driver as unavailable after matching")
        void testSelectedDriverBecomesUnavailable() {
            // Given
            Location pickupLocation = new Location(0, 0);

            Driver driver1 = new Driver("driver-1", new Location(3, 4));
            Driver driver2 = new Driver("driver-2", new Location(1, 0));

            List<Driver> availableDrivers = Arrays.asList(driver1, driver2);

            when(driverService.getAvailableDrivers()).thenReturn(availableDrivers);

            // When
            Driver nearest = matchingService.findNearestAvailableDriver(pickupLocation);

            // Then
            assertThat(nearest.isAvailable()).isFalse();
            verify(driverService, times(1)).getAvailableDrivers();
        }

        @Test
        @DisplayName("Should handle when only one driver is available")
        void testFindNearestWithSingleDriver() {
            // Given
            Location pickupLocation = new Location(0, 0);
            Driver driver = new Driver("single-driver", new Location(5, 5));

            List<Driver> availableDrivers = Collections.singletonList(driver);

            when(driverService.getAvailableDrivers()).thenReturn(availableDrivers);

            // When
            Driver nearest = matchingService.findNearestAvailableDriver(pickupLocation);

            // Then
            assertThat(nearest).isEqualTo(driver);
            assertThat(nearest.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("No Available Drivers Tests")
    class NoAvailableDriversTests {

        @Test
        @DisplayName("Should throw NotFoundException when no drivers available")
        void testNoDriversAvailable() {
            // Given
            Location pickupLocation = new Location(0, 0);

            when(driverService.getAvailableDrivers()).thenReturn(Collections.emptyList());

            // When & Then
            assertThatThrownBy(() -> matchingService.findNearestAvailableDriver(pickupLocation))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("No available drivers found");
        }

        @Test
        @DisplayName("Should throw NotFoundException when all drivers are unavailable")
        void testAllDriversUnavailable() {
            // Given
            Location pickupLocation = new Location(0, 0);

            Driver driver1 = new Driver("driver-1", new Location(3, 4));
            Driver driver2 = new Driver("driver-2", new Location(5, 12));

            // Mark both drivers as unavailable
            driver1.tryMarkUnavailable();
            driver2.tryMarkUnavailable();

            List<Driver> availableDrivers = Arrays.asList(driver1, driver2);

            when(driverService.getAvailableDrivers()).thenReturn(availableDrivers);

            // When & Then
            assertThatThrownBy(() -> matchingService.findNearestAvailableDriver(pickupLocation))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("No available drivers found");
        }
    }

    @Nested
    @DisplayName("Driver Becomes Unavailable After Allocation Tests")
    class DriverUnavailabilityTests {

        @Test
        @DisplayName("Driver should be unavailable after being matched to a ride")
        void testDriverUnavailableAfterAllocation() {
            // Given
            Location pickupLocation = new Location(0, 0);
            Driver driver = new Driver("allocated-driver", new Location(2, 3));

            assertThat(driver.isAvailable()).isTrue();

            List<Driver> availableDrivers = Collections.singletonList(driver);

            when(driverService.getAvailableDrivers()).thenReturn(availableDrivers);

            // When
            Driver matched = matchingService.findNearestAvailableDriver(pickupLocation);

            // Then
            assertThat(matched.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("Multiple allocation calls should only succeed once")
        void testMultipleAllocationAttempts() {
            // Given
            Location pickupLocation = new Location(0, 0);
            Driver driver = new Driver("driver-limited", new Location(1, 1));

            List<Driver> availableDrivers = Collections.singletonList(driver);

            when(driverService.getAvailableDrivers()).thenReturn(availableDrivers);

            // When - First allocation succeeds
            Driver first = matchingService.findNearestAvailableDriver(pickupLocation);
            assertThat(first.isAvailable()).isFalse();

            // Then - Second attempt should fail because driver is unavailable
            when(driverService.getAvailableDrivers()).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> matchingService.findNearestAvailableDriver(pickupLocation))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Fallback Logic Tests")
    class FallbackLogicTests {

        @Test
        @DisplayName("Should choose next closest driver if closest fails to mark unavailable")
        void testNextClosestDriverChosenOnFailure() {
            // Given
            Location pickupLocation = new Location(0, 0);

            // Driver 1: closest but will fail (already unavailable)
            Driver closestDriver = new Driver("closest-driver", new Location(1, 0));
            closestDriver.tryMarkUnavailable(); // Simulate failure - already unavailable

            // Driver 2: second closest (should be chosen)
            Driver secondClosestDriver = new Driver("second-closest-driver", new Location(2, 0));

            // Driver 3: farthest
            Driver farthestDriver = new Driver("farthest-driver", new Location(10, 0));

            List<Driver> availableDrivers = Arrays.asList(
                    closestDriver,
                    secondClosestDriver,
                    farthestDriver
            );

            when(driverService.getAvailableDrivers()).thenReturn(availableDrivers);

            // When
            Driver matched = matchingService.findNearestAvailableDriver(pickupLocation);

            // Then - Second closest should be chosen since closest already unavailable
            assertThat(matched).isEqualTo(secondClosestDriver);
            assertThat(matched.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("Should try multiple drivers until one succeeds")
        void testMultipleFailuresFallback() {
            // Given
            Location pickupLocation = new Location(0, 0);

            // Driver 1: closest (already unavailable - will fail)
            Driver driver1 = new Driver("driver-1", new Location(1, 0));
            driver1.tryMarkUnavailable();

            // Driver 2: middle distance (already unavailable - will fail)
            Driver driver2 = new Driver("driver-2", new Location(2, 0));
            driver2.tryMarkUnavailable();

            // Driver 3: farthest (available - should succeed)
            Driver driver3 = new Driver("driver-3", new Location(3, 0));

            List<Driver> availableDrivers = Arrays.asList(driver1, driver2, driver3);

            when(driverService.getAvailableDrivers()).thenReturn(availableDrivers);

            // When
            Driver matched = matchingService.findNearestAvailableDriver(pickupLocation);

            // Then
            assertThat(matched).isEqualTo(driver3);
            assertThat(matched.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("Should fail gracefully when all drivers fail to mark unavailable")
        void testAllDriversFailToMarkUnavailable() {
            // Given
            Location pickupLocation = new Location(0, 0);

            Driver driver1 = new Driver("driver-1", new Location(1, 0));
            Driver driver2 = new Driver("driver-2", new Location(2, 0));
            Driver driver3 = new Driver("driver-3", new Location(3, 0));

            // Mark all as unavailable
            driver1.tryMarkUnavailable();
            driver2.tryMarkUnavailable();
            driver3.tryMarkUnavailable();

            List<Driver> availableDrivers = Arrays.asList(driver1, driver2, driver3);

            when(driverService.getAvailableDrivers()).thenReturn(availableDrivers);

            // When & Then
            assertThatThrownBy(() -> matchingService.findNearestAvailableDriver(pickupLocation))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("No available drivers found");
        }
    }

    @Nested
    @DisplayName("Find Nearest Available Drivers (Limited) Tests")
    class FindNearestAvailableDriversTests {

        @Test
        @DisplayName("Should return drivers sorted by distance up to limit")
        void testFindNearestDriversWithLimit() {
            // Given
            Location pickupLocation = new Location(0, 0);

            Driver driver1 = new Driver("driver-1", new Location(1, 0));
            Driver driver2 = new Driver("driver-2", new Location(3, 4));
            Driver driver3 = new Driver("driver-3", new Location(5, 12));
            Driver driver4 = new Driver("driver-4", new Location(2, 0));

            List<Driver> availableDrivers = Arrays.asList(driver1, driver2, driver3, driver4);

            when(driverService.getAvailableDrivers()).thenReturn(availableDrivers);
            when(driverMapper.toDriverResponse(driver1))
                    .thenReturn(new DriverResponse("driver-1", 1, 0, true));
            when(driverMapper.toDriverResponse(driver4))
                    .thenReturn(new DriverResponse("driver-4", 2, 0, true));
            when(driverMapper.toDriverResponse(driver2))
                    .thenReturn(new DriverResponse("driver-2", 3, 4, true));

            // When
            List<DriverResponse> results = matchingService.findNearestAvailableDrivers(pickupLocation, 3);

            // Then
            assertThat(results).hasSize(3);
            assertThat(results.get(0).id()).isEqualTo("driver-1");
            assertThat(results.get(1).id()).isEqualTo("driver-4");
            assertThat(results.get(2).id()).isEqualTo("driver-2");
        }

        @Test
        @DisplayName("Should return all drivers when limit exceeds available count")
        void testLimitExceedsAvailableDrivers() {
            // Given
            Location pickupLocation = new Location(0, 0);

            Driver driver1 = new Driver("driver-1", new Location(1, 0));
            Driver driver2 = new Driver("driver-2", new Location(2, 0));

            List<Driver> availableDrivers = Arrays.asList(driver1, driver2);

            when(driverService.getAvailableDrivers()).thenReturn(availableDrivers);
            when(driverMapper.toDriverResponse(driver1))
                    .thenReturn(new DriverResponse("driver-1", 1, 0, true));
            when(driverMapper.toDriverResponse(driver2))
                    .thenReturn(new DriverResponse("driver-2", 2, 0, true));

            // When
            List<DriverResponse> results = matchingService.findNearestAvailableDrivers(pickupLocation, 10);

            // Then
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list when no drivers available")
        void testEmptyResultsWhenNoDriversAvailable() {
            // Given
            Location pickupLocation = new Location(0, 0);

            when(driverService.getAvailableDrivers()).thenReturn(Collections.emptyList());

            // When
            List<DriverResponse> results = matchingService.findNearestAvailableDrivers(pickupLocation, 5);

            // Then
            assertThat(results).isEmpty();
        }
    }
}
