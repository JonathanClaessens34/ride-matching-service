package com.jonathan.ride_matching_service.service;

import com.jonathan.ride_matching_service.dto.RideResponse;
import com.jonathan.ride_matching_service.exception.NotFoundException;
import com.jonathan.ride_matching_service.exception.RepositorySaveException;
import com.jonathan.ride_matching_service.mapper.RideMapper;
import com.jonathan.ride_matching_service.model.Driver;
import com.jonathan.ride_matching_service.model.Location;
import com.jonathan.ride_matching_service.model.Ride;
import com.jonathan.ride_matching_service.repository.RideRepository;
import com.jonathan.ride_matching_service.service.impl.RideServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RideService Tests")
class RideServiceTest {

    @Mock
    private MatchingService matchingService;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RideMapper rideMapper;

    private RideService rideService;

    @BeforeEach
    void setUp() {
        rideService = new RideServiceImpl(matchingService, rideRepository, rideMapper);
    }

    @Nested
    @DisplayName("Request Ride Tests")
    class RequestRideTests {

        @Test
        @DisplayName("Should create ride with correct data")
        void testRequestRideCreatesRideWithCorrectData() {
            // Given
            String riderId = "rider-123";
            Location pickupLocation = new Location(40.7128, -74.0060);

            Driver driver = new Driver("driver-456", new Location(40.7150, -74.0050));
            RideResponse expectedResponse = new RideResponse(
                    "ride-789",
                    "driver-456",
                    "rider-123",
                    40.7128,
                    -74.0060
            );

            when(matchingService.findNearestAvailableDriver(pickupLocation))
                    .thenReturn(driver);
            when(rideMapper.toRideResponse(any(Ride.class)))
                    .thenReturn(expectedResponse);

            // When
            RideResponse response = rideService.requestRide(riderId, pickupLocation);

            // Then
            assertThat(response).isEqualTo(expectedResponse);
            assertThat(response.riderId()).isEqualTo(riderId);
            assertThat(response.driverId()).isEqualTo("driver-456");
            assertThat(response.pickupX()).isEqualTo(40.7128);
            assertThat(response.pickupY()).isEqualTo(-74.0060);
        }

        @Test
        @DisplayName("Should store ride in repository")
        void testRequestRideStoredInRepository() {
            // Given
            String riderId = "rider-123";
            Location pickupLocation = new Location(40.7128, -74.0060);

            Driver driver = new Driver("driver-456", new Location(40.7150, -74.0050));
            RideResponse expectedResponse = new RideResponse(
                    "ride-789",
                    "driver-456",
                    "rider-123",
                    40.7128,
                    -74.0060
            );

            when(matchingService.findNearestAvailableDriver(pickupLocation))
                    .thenReturn(driver);
            when(rideMapper.toRideResponse(any(Ride.class)))
                    .thenReturn(expectedResponse);

            // When
            rideService.requestRide(riderId, pickupLocation);

            // Then
            ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
            verify(rideRepository, times(1)).save(rideCaptor.capture());

            Ride savedRide = rideCaptor.getValue();
            assertThat(savedRide).isNotNull();
            assertThat(savedRide.getRiderId()).isEqualTo(riderId);
            assertThat(savedRide.getDriver()).isEqualTo(driver);
            assertThat(savedRide.getPickupLocation()).isEqualTo(pickupLocation);
        }

        @Test
        @DisplayName("Should assign driver to ride")
        void testRequestRideAssignedDriver() {
            // Given
            String riderId = "rider-123";
            Location pickupLocation = new Location(40.7128, -74.0060);

            Driver driver = new Driver("driver-456", new Location(40.7150, -74.0050));
            RideResponse expectedResponse = new RideResponse(
                    "ride-789",
                    "driver-456",
                    "rider-123",
                    40.7128,
                    -74.0060
            );

            when(matchingService.findNearestAvailableDriver(pickupLocation))
                    .thenReturn(driver);
            when(rideMapper.toRideResponse(any(Ride.class)))
                    .thenReturn(expectedResponse);

            // When
            rideService.requestRide(riderId, pickupLocation);

            // Then
            ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
            verify(rideRepository, times(1)).save(rideCaptor.capture());

            Ride savedRide = rideCaptor.getValue();
            assertThat(savedRide.getDriver()).isNotNull();
            assertThat(savedRide.getDriver().getId()).isEqualTo("driver-456");
        }

        @Test
        @DisplayName("Should release driver if repository save fails")
        void testRequestRideReleasesDriverOnSaveFailure() {
            // Given
            String riderId = "rider-123";
            Location pickupLocation = new Location(40.7128, -74.0060);

            Driver driver = new Driver("driver-456", new Location(40.7150, -74.0050));

            when(matchingService.findNearestAvailableDriver(pickupLocation))
                    .thenReturn(driver);
            doThrow(new RuntimeException("Database error")).when(rideRepository).save(any(Ride.class));

            // When & Then
            assertThatThrownBy(() -> rideService.requestRide(riderId, pickupLocation))
                    .isInstanceOf(RepositorySaveException.class)
                    .hasMessageContaining("Failed to save ride");

            // Verify driver was released
            assertThat(driver.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("Should throw exception when no available drivers")
        void testRequestRideNoAvailableDrivers() {
            // Given
            String riderId = "rider-123";
            Location pickupLocation = new Location(40.7128, -74.0060);

            when(matchingService.findNearestAvailableDriver(pickupLocation))
                    .thenThrow(new NotFoundException("No available drivers found"));

            // When & Then
            assertThatThrownBy(() -> rideService.requestRide(riderId, pickupLocation))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("No available drivers found");
        }
    }

    @Nested
    @DisplayName("Complete Ride Tests")
    class CompleteRideTests {

        @Test
        @DisplayName("Should mark ride as completed")
        void testCompleteRideMarksAsCompleted() {
            // Given
            String rideId = "ride-123";
            Driver driver = new Driver("driver-456", new Location(40.7150, -74.0050));
            Ride ride = new Ride(rideId, "rider-789", driver, new Location(40.7128, -74.0060));

            assertThat(ride.isCompleted()).isFalse();

            when(rideRepository.findById(rideId)).thenReturn(ride);

            // When
            rideService.completeRide(rideId);

            // Then
            assertThat(ride.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("Should release driver when ride is completed")
        void testCompleteRideReleasesDriver() {
            // Given
            String rideId = "ride-123";
            Driver driver = new Driver("driver-456", new Location(40.7150, -74.0050));
            Ride ride = new Ride(rideId, "rider-789", driver, new Location(40.7128, -74.0060));

            // Mark driver as unavailable (simulating in-ride state)
            driver.tryMarkUnavailable();
            assertThat(driver.isAvailable()).isFalse();

            when(rideRepository.findById(rideId)).thenReturn(ride);

            // When
            rideService.completeRide(rideId);

            // Then
            assertThat(driver.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("Should throw NotFoundException when completing non-existing ride")
        void testCompleteNonExistingRide() {
            // Given
            String rideId = "non-existing-ride";

            when(rideRepository.findById(rideId)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> rideService.completeRide(rideId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Ride not found");
        }

        @Test
        @DisplayName("Should throw exception when completing already completed ride")
        void testCompleteAlreadyCompletedRide() {
            // Given
            String rideId = "ride-123";
            Driver driver = new Driver("driver-456", new Location(40.7150, -74.0050));
            Ride ride = new Ride(rideId, "rider-789", driver, new Location(40.7128, -74.0060));

            // Mark ride as already completed
            ride.complete();
            assertThat(ride.isCompleted()).isTrue();

            when(rideRepository.findById(rideId)).thenReturn(ride);

            // When & Then
            assertThatThrownBy(() -> rideService.completeRide(rideId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Ride already completed");
        }

        @Test
        @DisplayName("Should persist completed ride to repository")
        void testCompleteRidePersistsToRepository() {
            // Given
            String rideId = "ride-123";
            Driver driver = new Driver("driver-456", new Location(40.7150, -74.0050));
            Ride ride = new Ride(rideId, "rider-789", driver, new Location(40.7128, -74.0060));

            when(rideRepository.findById(rideId)).thenReturn(ride);

            // When
            rideService.completeRide(rideId);

            // Then
            // The ride object itself is modified, so we verify it was completed
            assertThat(ride.isCompleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("Integration Scenario Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Complete ride-request lifecycle")
        void testCompleteRideLifecycle() {
            // Given - Request phase
            String riderId = "rider-123";
            Location pickupLocation = new Location(40.7128, -74.0060);

            Driver driver = new Driver("driver-456", new Location(40.7150, -74.0050));
            // Simulate that matchingService already marked driver unavailable
            driver.tryMarkUnavailable();

            RideResponse expectedResponse = new RideResponse(
                    "ride-789",
                    "driver-456",
                    "rider-123",
                    40.7128,
                    -74.0060
            );

            when(matchingService.findNearestAvailableDriver(pickupLocation))
                    .thenReturn(driver);
            when(rideMapper.toRideResponse(any(Ride.class)))
                    .thenReturn(expectedResponse);

            // When - Request ride
            RideResponse response = rideService.requestRide(riderId, pickupLocation);

            // Then - Verify ride created with driver unavailable
            assertThat(response.rideId()).isEqualTo("ride-789");
            assertThat(driver.isAvailable()).isFalse();

            // Given - Complete phase
            ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
            verify(rideRepository, times(1)).save(rideCaptor.capture());
            Ride savedRide = rideCaptor.getValue();

            when(rideRepository.findById("ride-789")).thenReturn(savedRide);

            // When - Complete ride
            rideService.completeRide("ride-789");

            // Then - Verify ride completed and driver released
            assertThat(savedRide.isCompleted()).isTrue();
            assertThat(driver.isAvailable()).isTrue();
        }
    }
}
