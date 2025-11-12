package com.xynnity.watermanagement.device;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegistrationService.class);

    private final DeviceRegistrationRepository repository;

    public DeviceRegistrationService(DeviceRegistrationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<DeviceRegistration> findByDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return Optional.empty();
        }
        return repository.findByDeviceId(deviceId);
    }

    @Transactional(readOnly = true)
    public Optional<String> findClientIdByDevice(String deviceId) {
        return findByDeviceId(deviceId)
                .map(DeviceRegistration::getClientId)
                .filter(clientId -> !clientId.isBlank());
    }

    @Transactional
    public DeviceRegistration register(String deviceId, String clientId, String displayName) {
        var registration = repository.findByDeviceId(deviceId)
                .orElseGet(DeviceRegistration::new);
        registration.setDeviceId(deviceId);
        registration.setClientId(clientId);
        registration.setDisplayName(displayName);
        var saved = repository.save(registration);
        log.debug("Registered device mapping deviceId={}, clientId={}", saved.getDeviceId(), saved.getClientId());
        return saved;
    }
}

