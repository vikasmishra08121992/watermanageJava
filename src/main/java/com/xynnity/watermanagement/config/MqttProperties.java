package com.xynnity.watermanagement.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

@Validated
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {

    /**
     * Broker URI, e.g. tcp://host:1883.
     */
    @NotBlank
    private String brokerUri;

    /**
     * MQTT username.
     */
    @NotBlank
    private String username;

    /**
     * MQTT password.
     */
    @NotBlank
    private String password;

    /**
     * Client identifier used for outbound connections.
     */
    @NotBlank
    private String clientId = "watermanagement-app";

    /**
     * Default topic used when none is specified on publish.
     */
    @NotBlank
    private String defaultPublishTopic = "water/data";

    /**
     * Pattern used to compute publish topics when none is supplied explicitly.
     * Supports placeholders such as {@code {deviceId}} and {@code {clientId}}.
     */
    private String publishTopicPattern;

    /**
     * Topics to subscribe to on startup.
     */
    private List<@NotBlank String> subscriptionTopics = List.of("water/data");

    /**
     * 0-based index of the topic segment that represents the device id.
     * Set to {@code null} to disable extraction.
     */
    @PositiveOrZero
    private Integer topicDeviceIdIndex;

    /**
     * 0-based index of the topic segment that represents the client id.
     * Set to {@code null} to disable extraction.
     */
    @PositiveOrZero
    private Integer topicClientIdIndex;

    /**
     * Default QoS level for published messages.
     */
    @PositiveOrZero
    private int defaultQos = 0;

    /**
     * Number of inbound messages to keep in memory.
     */
    @PositiveOrZero
    private int messageCacheSize = 100;

    public String getBrokerUri() {
        return brokerUri;
    }

    public void setBrokerUri(String brokerUri) {
        this.brokerUri = brokerUri;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getDefaultPublishTopic() {
        return defaultPublishTopic;
    }

    public void setDefaultPublishTopic(String defaultPublishTopic) {
        this.defaultPublishTopic = defaultPublishTopic;
    }

    public List<String> getSubscriptionTopics() {
        return subscriptionTopics;
    }

    public void setSubscriptionTopics(List<String> subscriptionTopics) {
        this.subscriptionTopics = subscriptionTopics;
    }

    public int getDefaultQos() {
        return defaultQos;
    }

    public void setDefaultQos(int defaultQos) {
        this.defaultQos = defaultQos;
    }

    public int getMessageCacheSize() {
        return messageCacheSize;
    }

    public void setMessageCacheSize(int messageCacheSize) {
        this.messageCacheSize = messageCacheSize;
    }

    public Integer getTopicDeviceIdIndex() {
        return topicDeviceIdIndex;
    }

    public void setTopicDeviceIdIndex(Integer topicDeviceIdIndex) {
        this.topicDeviceIdIndex = topicDeviceIdIndex;
    }

    public Integer getTopicClientIdIndex() {
        return topicClientIdIndex;
    }

    public void setTopicClientIdIndex(Integer topicClientIdIndex) {
        this.topicClientIdIndex = topicClientIdIndex;
    }

    public String getPublishTopicPattern() {
        return publishTopicPattern;
    }

    public void setPublishTopicPattern(String publishTopicPattern) {
        this.publishTopicPattern = publishTopicPattern;
    }
}


