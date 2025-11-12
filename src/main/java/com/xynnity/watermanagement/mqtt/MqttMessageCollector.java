package com.xynnity.watermanagement.mqtt;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xynnity.watermanagement.config.MqttProperties;
import com.xynnity.watermanagement.device.DeviceEventService;
import com.xynnity.watermanagement.device.DeviceRegistrationService;

@Component
public class MqttMessageCollector {

    private static final String RECEIVED_CLIENT_ID_HEADER = "mqtt_receivedClientId";
    private static final Logger log = LoggerFactory.getLogger(MqttMessageCollector.class);

    private final DeviceEventService deviceEventService;
    private final ObjectMapper objectMapper;
    private final MqttProperties properties;
    private final DeviceRegistrationService deviceRegistrationService;

    public MqttMessageCollector(DeviceEventService deviceEventService,
                                ObjectMapper objectMapper,
                                MqttProperties properties,
                                DeviceRegistrationService deviceRegistrationService) {
        this.deviceEventService = deviceEventService;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.deviceRegistrationService = deviceRegistrationService;
    }

    public void store(Message<?> message) {
        var headers = message.getHeaders();
        var payload = message.getPayload() == null ? "" : message.getPayload().toString();
        var topic = header(headers, MqttHeaders.RECEIVED_TOPIC);
        var qos = header(headers, MqttHeaders.RECEIVED_QOS, Integer.class).orElse(0);
        var retained = header(headers, MqttHeaders.RECEIVED_RETAINED, Boolean.class).orElse(false);
        var clientId = header(headers, RECEIVED_CLIENT_ID_HEADER);

        var identifiers = resolveIdentifiers(topic, payload, clientId);
        var enrichedPayload = enrichPayload(payload, identifiers);
        log.debug("Received MQTT message topic={}, clientId={}, derivedDeviceId={}, payload={}",
                topic, identifiers.clientId(), identifiers.deviceId(), enrichedPayload);

        deviceEventService.recordEvent(
                topic,
                enrichedPayload,
                qos,
                retained,
                identifiers.deviceId(),
                identifiers.clientId());
    }

    private Identifiers resolveIdentifiers(String topic, String payload, String clientIdHeader) {
        String extractedDeviceId = null;
        String extractedClientId = normalize(clientIdHeader);

        if (StringUtils.hasText(payload)) {
            try {
                JsonNode root = objectMapper.readTree(payload);
                if (root.hasNonNull("deviceId")) {
                    extractedDeviceId = root.get("deviceId").asText();
                } else if (root.hasNonNull("device_id")) {
                    extractedDeviceId = root.get("device_id").asText();
                }
                if (root.hasNonNull("clientId")) {
                    extractedClientId = root.get("clientId").asText();
                } else if (root.hasNonNull("client_id")) {
                    extractedClientId = root.get("client_id").asText();
                }
            } catch (Exception ignored) {
                // payload is not JSON; ignore
                log.trace("Payload not parsed as JSON for topic {}: {}", topic, ignored.getMessage());
            }
        }

        if (!StringUtils.hasText(extractedDeviceId) && StringUtils.hasText(topic)) {
            extractedDeviceId = extractSegment(topic, properties.getTopicDeviceIdIndex());
        }
        if (!StringUtils.hasText(extractedClientId) && StringUtils.hasText(topic)) {
            extractedClientId = extractSegment(topic, properties.getTopicClientIdIndex());
        }

        final String deviceId = normalize(extractedDeviceId);
        String clientId = normalize(extractedClientId);

        if (!StringUtils.hasText(clientId) && StringUtils.hasText(deviceId)) {
            clientId = deviceRegistrationService.findClientIdByDevice(deviceId)
                    .or(() -> deviceEventService.latestClientIdForDevice(deviceId))
                    .orElse(null);
        }

        return new Identifiers(deviceId, normalize(clientId));
    }

    private String enrichPayload(String originalPayload, Identifiers identifiers) {
        Instant now = Instant.now();
        String timestamp = now.toString();
        String date = now.atZone(ZoneOffset.UTC).toLocalDate().toString();

        if (!StringUtils.hasText(originalPayload)) {
            return buildPayload(null, identifiers.deviceId(), identifiers.clientId(), date, timestamp);
        }

        try {
            JsonNode node = objectMapper.readTree(originalPayload);
            if (node.isObject()) {
                ObjectNode objectNode = (ObjectNode) node;
                objectNode.put("timestamp", timestamp);
                objectNode.put("date", date);
                if (StringUtils.hasText(identifiers.deviceId())) {
                    objectNode.put("deviceId", identifiers.deviceId());
                }
                if (StringUtils.hasText(identifiers.clientId())) {
                    objectNode.put("clientId", identifiers.clientId());
                }
                return objectMapper.writeValueAsString(objectNode);
            }
        } catch (Exception e) {
            log.trace("Unable to enrich payload as JSON, wrapping instead: {}", e.getMessage());
        }

        return buildPayload(originalPayload, identifiers.deviceId(), identifiers.clientId(), date, timestamp);
    }

    private String buildPayload(String value,
                                String deviceId,
                                String clientId,
                                String date,
                                String timestamp) {
        ObjectNode wrapper = objectMapper.createObjectNode();
        if (StringUtils.hasText(deviceId)) {
            wrapper.put("deviceId", deviceId);
        }
        if (StringUtils.hasText(clientId)) {
            wrapper.put("clientId", clientId);
        }
        wrapper.put("timestamp", timestamp);
        wrapper.put("date", date);
        if (value != null) {
            wrapper.put("payload", value);
        }
        return wrapper.toString();
    }

    private <T> Optional<T> header(MessageHeaders headers, String key, Class<T> type) {
        var value = headers.get(key);
        if (type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    private String header(MessageHeaders headers, String key) {
        return header(headers, key, String.class).map(String::valueOf).orElse("");
    }

    private String extractSegment(String topic, Integer index) {
        if (index == null || index < 0) {
            return null;
        }
        var segments = topic.split("/");
        if (index >= segments.length) {
            return null;
        }
        return normalize(segments[index]);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record Identifiers(String deviceId, String clientId) {
    }
}

