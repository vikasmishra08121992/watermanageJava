package com.xynnity.watermanagement.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import com.xynnity.watermanagement.mqtt.MqttMessageCollector;

@Configuration
public class MqttConfig {

    public static final String MQTT_INBOUND_CHANNEL = "mqttInboundChannel";
    public static final String MQTT_OUTBOUND_CHANNEL = "mqttOutboundChannel";

    @Bean
    public MqttConnectOptions mqttConnectOptions(MqttProperties properties) {
        var options = new MqttConnectOptions();
        options.setServerURIs(new String[] { properties.getBrokerUri() });
        options.setUserName(properties.getUsername());
        options.setPassword(properties.getPassword().toCharArray());
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setMaxInflight(10);
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);
        return options;
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory(MqttConnectOptions options) {
        var factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttInbound(MqttProperties properties,
                                       MqttPahoClientFactory clientFactory,
                                       MessageChannel mqttInboundChannel) {
        var subscriptionTopics = properties.getSubscriptionTopics();
        String[] topics;
        if (subscriptionTopics == null || subscriptionTopics.isEmpty()) {
            topics = new String[] { properties.getDefaultPublishTopic() };
        } else {
            topics = subscriptionTopics.toArray(String[]::new);
        }
        var adapter = new MqttPahoMessageDrivenChannelAdapter(
                properties.getClientId() + "-inbound",
                clientFactory,
                topics);
        adapter.setCompletionTimeout(5_000L);
        adapter.setConverter(defaultConverter());
        adapter.setQos(properties.getDefaultQos());
        adapter.setOutputChannel(mqttInboundChannel);
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = MQTT_OUTBOUND_CHANNEL)
    public MessageHandler mqttOutboundHandler(MqttProperties properties,
                                              MqttPahoClientFactory clientFactory) {
        var handler = new MqttPahoMessageHandler(properties.getClientId() + "-outbound", clientFactory);
        handler.setAsync(true);
        handler.setDefaultTopic(properties.getDefaultPublishTopic());
        handler.setDefaultQos(properties.getDefaultQos());
        handler.setDefaultRetained(false);
        handler.setConverter(defaultConverter());
        return handler;
    }

    @Bean
    @ServiceActivator(inputChannel = MQTT_INBOUND_CHANNEL)
    public MessageHandler mqttInboundMessageHandler(MqttMessageCollector collector) {
        return collector::store;
    }

    private DefaultPahoMessageConverter defaultConverter() {
        var converter = new DefaultPahoMessageConverter();
        converter.setPayloadAsBytes(false);
        return converter;
    }
}


