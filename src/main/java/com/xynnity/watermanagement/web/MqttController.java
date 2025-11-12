package com.xynnity.watermanagement.web;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xynnity.watermanagement.config.MqttProperties;
import com.xynnity.watermanagement.device.DeviceEventDto;
import com.xynnity.watermanagement.device.DeviceEventService;
import com.xynnity.watermanagement.mqtt.MqttGateway;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.CrossOrigin;

@Validated
@RestController
@RequestMapping("/api/mqtt")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class MqttController {

    private static final Logger log = LoggerFactory.getLogger(MqttController.class);

    private final MqttGateway mqttGateway;
    private final DeviceEventService deviceEventService;
    private final MqttProperties properties;
    private final ObjectMapper objectMapper;

    public MqttController(MqttGateway mqttGateway,
                          DeviceEventService deviceEventService,
                          MqttProperties properties,
                          ObjectMapper objectMapper) {
        this.mqttGateway = mqttGateway;
        this.deviceEventService = deviceEventService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/publish")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> publish(@Valid @RequestBody PublishRequest request) {
        var topic = resolveTopic(request);
        var qos = resolveQos(request.qos());
        var retained = Boolean.TRUE.equals(request.retained());
        var enrichedPayload = enrichPayload(request);
        log.debug("Publishing MQTT message: topic={}, qos={}, retained={}, payload={}, deviceId={}, clientId={}",
                topic, qos, retained, enrichedPayload, request.deviceId(), request.clientId());
        mqttGateway.sendToMqtt(topic, qos, retained, enrichedPayload);
        return Mono.empty();
    }

    @GetMapping("/messages")
    public Flux<DeviceEventDto> messages(@RequestParam(name = "deviceId", required = false) String deviceId,
                                         @RequestParam(name = "clientId", required = false) String clientId) {
        log.debug("Fetching stored events deviceId={}, clientId={}", deviceId, clientId);
        return Flux.fromIterable(deviceEventService.recentEvents(deviceId, clientId));
    }

    private String resolveTopic(PublishRequest request) {
        var pattern = properties.getPublishTopicPattern();
        if (StringUtils.hasText(pattern)) {
            return applyPattern(pattern, request);
        }
        var defaultTopic = properties.getDefaultPublishTopic();
        if (StringUtils.hasText(defaultTopic)) {
            return defaultTopic;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MQTT topic is required but no pattern or default is configured");
    }

    private int resolveQos(Integer qos) {
        if (Objects.isNull(qos)) {
            return properties.getDefaultQos();
        }
        if (qos < 0 || qos > 2) {
            log.warn("Invalid QoS {} requested for publish", qos);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QoS must be between 0 and 2");
        }
        return qos;
    }

    private String applyPattern(String pattern, PublishRequest request) {
        var resolved = pattern;
        resolved = replacePlaceholder(resolved, "deviceId", request.deviceId());
        resolved = replacePlaceholder(resolved, "clientId", request.clientId());
        if (resolved.contains("{")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unresolved placeholders remain in publish topic pattern");
        }
        return resolved;
    }

    private String replacePlaceholder(String template, String key, String value) {
        var placeholder = "{" + key + "}";
        if (!template.contains(placeholder)) {
            return template;
        }
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Missing value for " + key + " required by publish topic pattern");
        }
        return template.replace(placeholder, value);
    }

    private String enrichPayload(PublishRequest request) {
        try {
            JsonNode node = objectMapper.readTree(request.payload());
            if (node.isObject()) {
                ObjectNode objectNode = (ObjectNode) node;
                objectNode.put("deviceId", request.deviceId());
                if (StringUtils.hasText(request.clientId())) {
                    objectNode.put("clientId", request.clientId());
                }
                return objectMapper.writeValueAsString(objectNode);
            }
        } catch (Exception e) {
            log.trace("Payload is not JSON object, wrapping with metadata: {}", e.getMessage());
        }
        ObjectNode wrapper = objectMapper.createObjectNode();
        wrapper.put("deviceId", request.deviceId());
        if (StringUtils.hasText(request.clientId())) {
            wrapper.put("clientId", request.clientId());
        }
        wrapper.put("payload", request.payload());
        return wrapper.toString();
    }

    public record PublishRequest(
            @NotBlank(message = "Payload is required")
            String payload,
            @Min(0) @Max(2)
            Integer qos,
            Boolean retained,
            @NotBlank(message = "deviceId is required")
            String deviceId,
            String clientId) {
    }
}


