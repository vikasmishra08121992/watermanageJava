package com.xynnity.watermanagement.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xynnity.watermanagement.device.DeviceEventService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ServerWebSocket("/api/events/ws")
public class EventsWebSocketController implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(EventsWebSocketController.class);

    private final DeviceEventService deviceEventService;
    private final ObjectMapper objectMapper;
    private final DeviceSubscriptionRegistry subscriptionRegistry;

    public EventsWebSocketController(DeviceEventService deviceEventService,
                                     ObjectMapper objectMapper,
                                     DeviceSubscriptionRegistry subscriptionRegistry) {
        this.deviceEventService = deviceEventService;
        this.objectMapper = objectMapper;
        this.subscriptionRegistry = subscriptionRegistry;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        var params = queryParams(session);
        var deviceId = params.getFirst("deviceId");
        var clientId = params.getFirst("clientId");
        log.debug("WebSocket session {} connected deviceId={}, clientId={}", session.getId(), deviceId, clientId);
        subscriptionRegistry.register(session.getId(), deviceId, clientId);
        var updates = deviceEventService.stream(deviceId, clientId)
                .bufferTimeout(10, Duration.ofSeconds(1))
                .filter(batch -> !batch.isEmpty())
                .flatMap(batch -> Mono.fromCallable(() -> serialize(batch))
                        .doOnNext(json -> log.trace("Streaming batch to session {}: {}", session.getId(), json))
                        .map(session::textMessage));

         var outbound = updates;


        var receive = session.receive()
                .doOnNext(WebSocketMessage::release)
                .doOnNext(msg -> log.trace("Ignoring inbound message session={}, payload={}", session.getId(), msg.getPayloadAsText()))
                .then()
                .doFinally(signal -> {
                    subscriptionRegistry.unregister(session.getId());
                    log.debug("WebSocket session {} closed ({})", session.getId(), signal);
                });

        return session.send(outbound).and(receive);
    }

    private MultiValueMap<String, String> queryParams(WebSocketSession session) {
        return UriComponentsBuilder.fromUri(session.getHandshakeInfo().getUri())
                .build()
                .getQueryParams();
    }

    private String serialize(Object value) throws Exception {
        if (value instanceof List<?> list) {
            return objectMapper.writeValueAsString(list);
        }
        return objectMapper.writeValueAsString(value);
    }
}


