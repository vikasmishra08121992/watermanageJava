package com.xynnity.watermanagement.device;

import java.util.List;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceEventRepository extends JpaRepository<DeviceEvent, Long> {

    List<DeviceEvent> findTop50ByOrderByReceivedAtDesc();

    List<DeviceEvent> findTop50ByDeviceIdOrderByReceivedAtDesc(String deviceId);

    List<DeviceEvent> findTop50ByClientIdOrderByReceivedAtDesc(String clientId);

    List<DeviceEvent> findTop50ByDeviceIdAndClientIdOrderByReceivedAtDesc(String deviceId, String clientId);

    boolean existsByDeviceIdAndClientId(String deviceId, String clientId);

    Optional<DeviceEvent> findFirstByDeviceIdOrderByReceivedAtDesc(String deviceId);
}


