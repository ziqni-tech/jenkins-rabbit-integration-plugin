package com.ziqni.jenkins.plugins.rabbit.utils;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import com.ziqni.jenkins.plugins.rabbit.trigger.RabbitBuildCause;
import hudson.model.Run;

import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

public class RabbitMessageProperties {

    private final Map<String, String> vars;

    private final String queueName;
    private final String appId;
    private final AMQP.BasicProperties properties;

    public static final String RABBIT_QUEUE_NAME = "RABBIT_QUEUE_NAME";
    public static final String RABBIT_EXCHANGE = "RABBIT_EXCHANGE";
    public static final String RABBIT_ROUTING_KEY = "RABBIT_ROUTING_KEY";
    public static final String RABBIT_DELIVERY_TAG = "RABBIT_DELIVERY_TAG";
    public static final String RABBIT_REDELIVERED = "RABBIT_REDELIVERED";

    public static final String RABBIT_CONTENT_TYPE = "RABBIT_CONTENT_TYPE";
    public static final String RABBIT_CONTENT_ENCODING = "RABBIT_CONTENT_ENCODING";
    public static final String RABBIT_HEADER_ = "RABBIT_HEADER_";
    public static final String RABBIT_DELIVERY_MODE = "RABBIT_DELIVERY_MODE";
    public static final String RABBIT_PRIORITY = "RABBIT_PRIORITY";
    public static final String RABBIT_CORRELATION_ID = "RABBIT_CORRELATION_ID";
    public static final String RABBIT_REPLY_TO = "RABBIT_REPLY_TO";
    public static final String RABBIT_EXPIRATION = "RABBIT_EXPIRATION";
    public static final String RABBIT_MESSAGE_ID = "RABBIT_MESSAGE_ID";
    public static final String RABBIT_TIMESTAMP = "RABBIT_TIMESTAMP";
    public static final String RABBIT_TYPE = "RABBIT_TYPE";
    public static final String RABBIT_USER_ID = "RABBIT_USER_ID";
    public static final String RABBIT_APP_ID = "RABBIT_APP_ID";
    public static final String RABBIT_CLUSTER_ID = "RABBIT_CLUSTER_ID";

    public RabbitMessageProperties(String appId, String queueName, Envelope envelope, AMQP.BasicProperties properties) {
        Objects.requireNonNull(envelope, "Envelope cannot be null");

        this.appId = appId;
        this.properties = Objects.requireNonNull(properties, "Properties cannot be null");
        this.queueName = Objects.requireNonNull(queueName, "Queue name cannot be null");
        this.vars = new HashMap<>();

        // Initialize the vars map with values from properties and envelope
        populateVars(envelope);
    }


    public RabbitMessageProperties(String queueName, Envelope envelope, AMQP.BasicProperties properties) {
        this(properties.getAppId(), queueName, envelope, properties);
    }

    private void populateVars(Envelope envelope) {
        // Add values from BasicProperties to the vars map
        putIfNotNull(RABBIT_APP_ID, this.appId);

        putIfNotNull(RABBIT_CONTENT_TYPE, properties.getContentType());
        putIfNotNull(RABBIT_CONTENT_ENCODING, properties.getContentEncoding());
        putIfNotNull(RABBIT_DELIVERY_MODE, properties.getDeliveryMode());
        putIfNotNull(RABBIT_PRIORITY, properties.getPriority());
        putIfNotNull(RABBIT_CORRELATION_ID, properties.getCorrelationId());
        putIfNotNull(RABBIT_REPLY_TO, properties.getReplyTo());
        putIfNotNull(RABBIT_EXPIRATION, properties.getExpiration());
        putIfNotNull(RABBIT_MESSAGE_ID, properties.getMessageId());
        putIfNotNull(RABBIT_TIMESTAMP, properties.getTimestamp());
        putIfNotNull(RABBIT_TYPE, properties.getType());
        putIfNotNull(RABBIT_USER_ID, properties.getUserId());
        putIfNotNull(RABBIT_CLUSTER_ID, properties.getClusterId());

        // Add headers as separate entries with the RABBIT_HEADER_ prefix
        Optional.ofNullable(properties.getHeaders()).ifPresent(headers -> {
            headers.forEach((key, value) -> {
                String headerKey = RABBIT_HEADER_ + key.toUpperCase(); // To keep the naming consistent
                putIfNotNull(headerKey, value);
            });
        });

        // Add values from Envelope to the vars map
        vars.put(RABBIT_QUEUE_NAME, queueName);
        vars.put(RABBIT_EXCHANGE, envelope.getExchange());
        vars.put(RABBIT_ROUTING_KEY, envelope.getRoutingKey());
        vars.put(RABBIT_DELIVERY_TAG, String.valueOf(envelope.getDeliveryTag()));
        vars.put(RABBIT_REDELIVERED, String.valueOf(envelope.isRedeliver()));
    }

    /**
     * Get the appId.
     * @return the appId.
     */
    public String getAppId() {
        return appId;
    }

    private void putIfNotNull(String key, Object value) {
        if (value != null) {
            vars.put(key, value.toString());
        }
    }

    public Map<String, String> getVars() {
        return vars;
    }

    public String getQueueName() {
        return queueName;
    }

    public AMQP.BasicProperties getProperties() {
        return properties;
    }

    public String getValue(String key) {
        // Return the value directly from the vars map if available
        return vars.get(key.toUpperCase());
    }

    public Map<String, Object> getHeaders() {
        return properties.getHeaders();
    }

    public static RabbitMessageProperties get(Run<?,?> run) {

        // Retrieve the RabbitBuildCause from the build
        RabbitBuildCause cause = run.getCause(RabbitBuildCause.class);

        if (cause != null) {
            // Access RabbitMessageProperties from the cause
            return cause.getRabbitMessageProperties();
        }
        else {
            return new RabbitMessageProperties("unknown", new Envelope(1, false, "unknown", "unknown"), new AMQP.BasicProperties());
        }
    }
}
