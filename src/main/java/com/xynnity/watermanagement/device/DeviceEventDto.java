package com.xynnity.watermanagement.device;

import java.time.Instant;

public record DeviceEventDto(
        Long id,
        String deviceId,
        String clientId,
        String topic,
        String payload,
        int qos,
        boolean retained,
        Instant receivedAt) {
}


