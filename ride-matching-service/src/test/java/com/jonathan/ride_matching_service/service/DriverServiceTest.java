package com.jonathan.ride_matching_service.service;

import com.jonathan.ride_matching_service.dto.DriverResponse;
import com.jonathan.ride_matching_service.exception.NotFoundException;
import com.jonathan.ride_matching_service.exception.RepositorySaveException;
import com.jonathan.ride_matching_service.mapper.DriverMapper;
import com.jonathan.ride_matching_service.model.Driver;
import com.jonathan.ride_matching_service.model.Location;
import com.jonathan.ride_matching_service.repository.DriverRepository;
import com.jonathan.ride_matching_service.service.impl.DriverServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DriverService Tests")
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private DriverMapper driverMapper;

    private DriverService driverService;

    @BeforeEach
    void setUp() {
        driverService = new DriverServiceImpl(driverRepository, driverMapper);
    }

    @Nested
    @DisplayName("Register Driver Tests")
    class RegisterDriverTests {

        @Test
        @DisplayName("Should register driver with valid id and location")
        void testRegisterDriverWithValidInput() {
            // Given
            String driverId = "driver-123";
            Location location = new Location(40.7128, -74.0060);

            when(driverRepository.findById(driverId)).thenReturn(null);

            // When
            driverService.registerDriver(driverId, location);

            // Then
            verify(driverRepository, times(1)).save(argThat(driver ->
                    driver.getId().equals(driverId) &&
                    driver.getLocation().equals(location)
            ));
        }

        @Test
        @DisplayName("Driver should be available by default after registration")
        void testDriverAvailableByDefault() {
            // Given
            String driverId = "driver-456";
            Location location = new Location(40.7128, -74.0060);

            when(driverRepository.findById(driverId)).thenReturn(null);

            // When
            driverService.registerDriver(driverId, location);

            // Then
            verify(driverRepository, times(1)).save(argThat(driver ->
                    driver.isAvailable()
            ));
        }

        @Test
        @DisplayName("Should throw exception when driver already exists")
        void testRegisterExistingDriver() {
            // Given
            String driverId = "driver-789";
            Location location = new Location(40.7128, -74.0060);
            Driver existingDriver = new Driver(driverId, location);

            when(driverRepository.findById(driverId)).thenReturn(existingDriver);

            // When & Then
            assertThatThrownBy(() -> driverService.registerDriver(driverId, location))
                    .isInstanceOf(RepositorySaveException.class)
                    .hasMessageContaining("Failed to register driver");
        }

        @Test
        @DisplayName("Should throw RepositorySaveException on save failure")
        void testRegisterDriverSaveFailure() {
            // Given
            String driverId = "driver-save-fail";
            Location location = new Location(40.7128, -74.0060);

            when(driverRepository.findById(driverId)).thenReturn(null);
            doThrow(new RuntimeException("Database error")).when(driverRepository).save(any());

            // When & Then
            assertThatThrownBy(() -> driverService.registerDriver(driverId, location))
                    .isInstanceOf(RepositorySaveException.class)
                    .hasMessageContaining("Failed to register driver");
        }
    }

    @Nested
    @DisplayName("Update Driver Tests")
    class UpdateDriverTests {

        @Test
        @DisplayName("Should update driver location correctly")
        void testUpdateDriverLocation() {
            // Given
            String driverId = "driver-update-loc";
            Location oldLocation = new Location(40.7128, -74.0060);
            Location newLocation = new Location(40.7580, -73.9855);

            Driver driver = new Driver(driverId, oldLocation);
            DriverResponse expectedResponse = new DriverResponse(driverId, newLocation.x(), newLocation.y(), true);

            when(driverRepository.findById(driverId)).thenReturn(driver);
            when(driverMapper.toDriverResponse(driver)).thenReturn(expectedResponse);

            // When
            DriverResponse response = driverService.updateDriver(driverId, newLocation, true);

            // Then
            assertThat(driver.getLocation()).isEqualTo(newLocation);
            assertThat(response).isEqualTo(expectedResponse);
            verify(driverMapper, times(1)).toDriverResponse(driver);
        }

        @Test
        @DisplayName("Should update driver availability correctly")
        void testUpdateDriverAvailability() {
            // Given
            String driverId = "driver-update-avail";
            Location location = new Location(40.7128, -74.0060);

            Driver driver = new Driver(driverId, location);
            DriverResponse expectedResponse = new DriverResponse(driverId, location.x(), location.y(), false);

            when(driverRepository.findById(driverId)).thenReturn(driver);
            when(driverMapper.toDriverResponse(driver)).thenReturn(expectedResponse);

            // When
            DriverResponse response = driverService.updateDriver(driverId, location, false);

            // Then
            assertThat(driver.isAvailable()).isFalse();
            assertThat(response).isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("Should throw NotFoundException when updating non-existing driver")
        void testUpdateNonExistingDriver() {
            // Given
            String driverId = "non-existing-driver";
            Location location = new Location(40.7128, -74.0060);

            when(driverRepository.findById(driverId)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> driverService.updateDriver(driverId, location, true))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Driver not found with id: " + driverId);
        }

        @Test
        @DisplayName("Should update both location and availability together")
        void testUpdateLocationAndAvailabilityTogether() {
            // Given
            String driverId = "driver-update-both";
            Location oldLocation = new Location(40.7128, -74.0060);
            Location newLocation = new Location(40.7580, -73.9855);

            Driver driver = new Driver(driverId, oldLocation);
            DriverResponse expectedResponse = new DriverResponse(driverId, newLocation.x(), newLocation.y(), false);

            when(driverRepository.findById(driverId)).thenReturn(driver);
            when(driverMapper.toDriverResponse(driver)).thenReturn(expectedResponse);

            // When
            DriverResponse response = driverService.updateDriver(driverId, newLocation, false);

            // Then
            assertThat(driver.getLocation()).isEqualTo(newLocation);
            assertThat(driver.isAvailable()).isFalse();
            assertThat(response).isEqualTo(expectedResponse);
        }
    }

    @Nested
    @DisplayName("Availability Logic Tests")
    class AvailabilityLogicTests {

        @Test
        @DisplayName("Should return false when driver is already unavailable and tryMarkUnavailable is called")
        void testTryMarkUnavailableWhenAlreadyUnavailable() {
            // Given
            String driverId = "driver-unavail";
            Location location = new Location(40.7128, -74.0060);
            Driver driver = new Driver(driverId, location);

            // Mark driver as unavailable first
            driver.tryMarkUnavailable();

            // When
            boolean result = driver.tryMarkUnavailable();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when driver is available and tryMarkUnavailable is called")
        void testTryMarkUnavailableWhenAvailable() {
            // Given
            String driverId = "driver-avail";
            Location location = new Location(40.7128, -74.0060);
            Driver driver = new Driver(driverId, location);

            // Driver is available by default
            assertThat(driver.isAvailable()).isTrue();

            // When
            boolean result = driver.tryMarkUnavailable();

            // Then
            assertThat(result).isTrue();
            assertThat(driver.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("Should release driver from unavailable state")
        void testReleaseDriver() {
            // Given
            String driverId = "driver-release";
            Location location = new Location(40.7128, -74.0060);
            Driver driver = new Driver(driverId, location);

            // Mark as unavailable
            driver.tryMarkUnavailable();
            assertThat(driver.isAvailable()).isFalse();

            // When
            driver.release();

            // Then
            assertThat(driver.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("Should return true for consecutive tryMarkUnavailable calls with release in between")
        void testConsecutiveMarkUnavailableWithRelease() {
            // Given
            String driverId = "driver-consecutive";
            Location location = new Location(40.7128, -74.0060);
            Driver driver = new Driver(driverId, location);

            // When & Then
            assertThat(driver.tryMarkUnavailable()).isTrue();
            assertThat(driver.isAvailable()).isFalse();

            driver.release();
            assertThat(driver.isAvailable()).isTrue();

            assertThat(driver.tryMarkUnavailable()).isTrue();
            assertThat(driver.isAvailable()).isFalse();
        }
    }
}
