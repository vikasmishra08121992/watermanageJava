package com.xynnity.watermanagement.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;

import static com.xynnity.watermanagement.config.MqttConfig.MQTT_OUTBOUND_CHANNEL;

@MessagingGateway(defaultRequestChannel = MQTT_OUTBOUND_CHANNEL)
public interface MqttGateway {

    Logger log = LoggerFactory.getLogger(MqttGateway.class);

    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic,
                    @Header(MqttHeaders.QOS) int qos,
                    @Header(name = MqttHeaders.RETAINED, defaultValue = "false") boolean retained,
                    String payload);

    default void sendToMqtt(String topic, int qos, String payload) {
        log.debug("Sending MQTT message: topic={}, qos={}, payload={}", topic, qos, payload);
        sendToMqtt(topic, qos, false, payload);
    }
}


