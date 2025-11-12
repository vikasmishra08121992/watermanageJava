package com.xynnity.watermanagement.device;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRegistrationRepository extends JpaRepository<DeviceRegistration, Long> {

    Optional<DeviceRegistration> findByDeviceId(String deviceId);
}

