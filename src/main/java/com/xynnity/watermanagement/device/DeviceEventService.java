package com.xynnity.watermanagement.device;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xynnity.watermanagement.websocket.DeviceSubscriptionRegistry;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class DeviceEventService {

    private static final Logger log = LoggerFactory.getLogger(DeviceEventService.class);

    private final DeviceEventRepository repository;
    private final Sinks.Many<DeviceEventDto> sink;
    private final DeviceSubscriptionRegistry subscriptionRegistry;

    public DeviceEventService(DeviceEventRepository repository,
                              DeviceSubscriptionRegistry subscriptionRegistry) {
        this.repository = repository;
        this.subscriptionRegistry = subscriptionRegistry;
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
    }

    @Transactional
    public DeviceEventDto recordEvent(String topic,
                                      String payload,
                                      Integer qos,
                                      Boolean retained,
                                      String deviceId,
                                      String clientId) {
        return recordEvent(topic, payload, qos, retained, deviceId, clientId, Instant.now());
    }

    @Transactional
    public DeviceEventDto recordEvent(String topic,
                                      String payload,
                                      Integer qos,
                                      Boolean retained,
                                      String deviceId,
                                      String clientId,
                                      Instant receivedAt) {
        var event = new DeviceEvent();
        event.setTopic(topic);
        event.setPayload(payload);
        event.setQos(qos != null ? qos : 0);
        event.setRetained(Boolean.TRUE.equals(retained));
        event.setDeviceId(normalize(deviceId));
        event.setClientId(normalize(clientId));
        event.setReceivedAt(receivedAt != null ? receivedAt : Instant.now());

        log.debug("Persisting device event topic={}, deviceId={}, clientId={}, qos={}, retained={}",
                topic, event.getDeviceId(), event.getClientId(), event.getQos(), event.isRetained());

        var saved = repository.save(event);
        var dto = toDto(saved);
        if (subscriptionRegistry.hasSubscribers(saved.getDeviceId(), saved.getClientId())) {
            sink.tryEmitNext(dto);
            log.trace("Emitted device event ID={} to sink", dto.id());
        } else {
            log.trace("No active subscriptions for deviceId={}, clientId={}, skipping sink emission",
                    saved.getDeviceId(), saved.getClientId());
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public List<DeviceEventDto> recentEvents(String deviceId, String clientId) {
        List<DeviceEvent> events;
        if (deviceId != null && !deviceId.isBlank() && clientId != null && !clientId.isBlank()) {
            events = repository.findTop50ByDeviceIdAndClientIdOrderByReceivedAtDesc(deviceId, clientId);
        } else if (deviceId != null && !deviceId.isBlank()) {
            events = repository.findTop50ByDeviceIdOrderByReceivedAtDesc(deviceId);
        } else if (clientId != null && !clientId.isBlank()) {
            events = repository.findTop50ByClientIdOrderByReceivedAtDesc(clientId);
        } else {
            events = repository.findTop50ByOrderByReceivedAtDesc();
        }
        log.debug("Loaded {} events for deviceId={}, clientId={}", events.size(), deviceId, clientId);
        return events.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<String> latestClientIdForDevice(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return java.util.Optional.empty();
        }
        return repository.findFirstByDeviceIdOrderByReceivedAtDesc(deviceId)
                .map(DeviceEvent::getClientId)
                .filter(client -> client != null && !client.isBlank());
    }

    public Flux<DeviceEventDto> stream(String deviceId, String clientId) {
        return sink.asFlux()
                .filter(event -> matches(event, deviceId, clientId));
    }

    public Flux<DeviceEventDto> streamWithHistory(String deviceId, String clientId) {
        return Flux.defer(() -> Flux.fromIterable(recentEvents(deviceId, clientId)))
                .concatWith(stream(deviceId, clientId));
    }

    private DeviceEventDto toDto(DeviceEvent event) {
        return new DeviceEventDto(
                event.getId(),
                event.getDeviceId(),
                event.getClientId(),
                event.getTopic(),
                event.getPayload(),
                event.getQos(),
                event.isRetained(),
                event.getReceivedAt());
    }

    private boolean matches(DeviceEventDto event, String deviceId, String clientId) {
        if (deviceId != null && !deviceId.isBlank() && !Objects.equals(deviceId, event.deviceId())) {
            return false;
        }
        if (clientId != null && !clientId.isBlank() && !Objects.equals(clientId, event.clientId())) {
            return false;
        }
        return true;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}


