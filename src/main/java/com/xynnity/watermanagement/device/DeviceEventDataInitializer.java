package com.xynnity.watermanagement.device;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DeviceEventDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DeviceEventDataInitializer.class);

    private static final String MOCK_DEVICE_ID = "device-007";
    private static final String MOCK_CLIENT_ID = "panel-west";
    private static final String MOCK_TOPIC = "water/" + MOCK_DEVICE_ID + "/data";

    private final DeviceEventRepository repository;
    private final DeviceEventService deviceEventService;
    private final ObjectMapper objectMapper;

    public DeviceEventDataInitializer(DeviceEventRepository repository,
                                      DeviceEventService deviceEventService,
                                      ObjectMapper objectMapper) {
        this.repository = repository;
        this.deviceEventService = deviceEventService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (repository.existsByDeviceIdAndClientId(MOCK_DEVICE_ID, MOCK_CLIENT_ID)) {
            log.debug("Mock data for {} / {} already present; skipping initialization", MOCK_DEVICE_ID, MOCK_CLIENT_ID);
            return;
        }

        var readings = buildMockReadings();
        for (MockReading reading : readings) {
            var payload = objectMapper.writeValueAsString(Map.of(
                    "usage", reading.usage,
                    "timestamp", reading.timestamp.toString()
            ));
            deviceEventService.recordEvent(
                    MOCK_TOPIC,
                    payload,
                    0,
                    false,
                    MOCK_DEVICE_ID,
                    MOCK_CLIENT_ID,
                    reading.timestamp);
        }
        log.info("Inserted {} mock readings for device {} / {}", readings.size(), MOCK_DEVICE_ID, MOCK_CLIENT_ID);
    }

    private List<MockReading> buildMockReadings() {
        List<MockReading> readings = new ArrayList<>();
        readings.addAll(readingsForDay(2025, 11, 6, new double[] { 18.5, 21.0, 22.4, 19.8, 20.7 }));
        readings.addAll(readingsForDay(2025, 11, 7, new double[] { 23.1, 24.6, 26.2, 25.4, 23.9 }));
        readings.addAll(readingsForDay(2025, 11, 8, new double[] { 21.7, 20.3, 19.9, 22.5, 24.2 }));
        readings.addAll(readingsForDay(2025, 11, 9, new double[] { 25.6, 27.1, 26.8, 24.9, 23.3 }));
        return readings;
    }

    private List<MockReading> readingsForDay(int year, int month, int day, double[] usages) {
        List<MockReading> readings = new ArrayList<>();
        for (int i = 0; i < usages.length; i++) {
            LocalDateTime dateTime = LocalDateTime.of(year, month, day, 6 + (i * 3), 15);
            Instant timestamp = dateTime.toInstant(ZoneOffset.UTC);
            readings.add(new MockReading(timestamp, usages[i] + "cc"));
        }
        return readings;
    }

    private record MockReading(Instant timestamp, String usage) {
    }
}


