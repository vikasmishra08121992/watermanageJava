package com.xynnity.watermanagement.websocket;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DeviceSubscriptionRegistry {

    private static final Logger log = LoggerFactory.getLogger(DeviceSubscriptionRegistry.class);

    private final ConcurrentHashMap<SubscriptionKey, Set<String>> subscriptions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SubscriptionKey> sessionIndex = new ConcurrentHashMap<>();

    public void register(String sessionId, String deviceId, String clientId) {
        var key = SubscriptionKey.of(deviceId, clientId);
        subscriptions.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionIndex.put(sessionId, key);
        log.trace("Registered WebSocket session {} for key {}", sessionId, key);
    }

    public void unregister(String sessionId) {
        var key = sessionIndex.remove(sessionId);
        if (key == null) {
            return;
        }
        var sessions = subscriptions.get(key);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                subscriptions.remove(key, sessions);
            }
        }
        log.trace("Unregistered WebSocket session {} for key {}", sessionId, key);
    }

    public boolean hasSubscribers(String deviceId, String clientId) {
        return possibleKeys(deviceId, clientId)
                .map(subscriptions::get)
                .filter(Objects::nonNull)
                .anyMatch(set -> !set.isEmpty());
    }

    private static java.util.stream.Stream<SubscriptionKey> possibleKeys(String deviceId, String clientId) {
        return java.util.stream.Stream.of(
                        SubscriptionKey.of(deviceId, clientId),
                        SubscriptionKey.of(deviceId, null),
                        SubscriptionKey.of(null, clientId),
                        SubscriptionKey.of(null, null))
                .distinct();
    }

    private record SubscriptionKey(String deviceId, String clientId) {

        private static SubscriptionKey of(String deviceId, String clientId) {
            return new SubscriptionKey(normalize(deviceId), normalize(clientId));
        }

        private static String normalize(String value) {
            return Optional.ofNullable(value)
                    .map(String::trim)
                    .filter(text -> !text.isBlank())
                    .orElse(null);
        }

        @Override
        public String toString() {
            return "SubscriptionKey{deviceId='%s', clientId='%s'}"
                    .formatted(deviceId, clientId);
        }
    }
}

