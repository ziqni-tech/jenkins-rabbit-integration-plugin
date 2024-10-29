package com.ziqni.jenkins.plugins.rabbit.utils;

import com.rabbitmq.client.AMQP;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * RabbitMessageBuilder class to build AMQP.BasicProperties.Builder object
 */
public abstract class RabbitMessageBuilder {

    public static AMQP.BasicProperties.Builder build(AMQP.BasicProperties.Builder builder, RabbitMessageBuilderAccessors accessors, Map<String,Object> headersMap){
        if (Objects.nonNull(accessors.getContentType())) {
            builder.contentType(accessors.getContentType());
        }
        if (Objects.nonNull(accessors.getContentEncoding())) {
            builder.contentEncoding(accessors.getContentEncoding());
        }
        if (Objects.nonNull(accessors.getDeliveryMode())) {
            builder.deliveryMode(accessors.getDeliveryMode());
        }
        if (Objects.nonNull(accessors.getPriority())) {
            builder.priority(accessors.getPriority());
        }
        if (Objects.nonNull(accessors.getCorrelationId())) {
            builder.correlationId(accessors.getCorrelationId());
        }
        if (Objects.nonNull(accessors.getReplyTo())) {
            builder.replyTo(accessors.getReplyTo());
        }
        if (Objects.nonNull(accessors.getExpiration())) {
            builder.expiration(accessors.getExpiration());
        }
        if (Objects.nonNull(accessors.getMessageId())) {
            builder.messageId(accessors.getMessageId());
        }
        if (Objects.nonNull(accessors.getTimestamp()) && !accessors.getTimestamp().isEmpty()) {
            try {
                builder.timestamp(DateFormat.getDateInstance().parse(accessors.getTimestamp()));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        if (Objects.nonNull(accessors.getType())) {
            builder.type(accessors.getType());
        }
        if (Objects.nonNull(accessors.getUserId())) {
            builder.userId(accessors.getUserId());
        }
        if (Objects.nonNull(accessors.getAppId())) {
            builder.appId(accessors.getAppId());
        }
        if (Objects.nonNull(accessors.getClusterId())) {
            builder.clusterId(accessors.getClusterId());
        }

        // Add headers if present
        if (Objects.nonNull(accessors.getHeaders())) {
            builder.headers(parseHeaders(accessors.getHeaders(), headersMap));
        }
        else if (Objects.nonNull(headersMap)) {
            builder.headers(headersMap);
        }

        return builder;
    }


    // Method to parse headers from a JSON string or key=value format to a Map<String, Object>
    public static Map<String, Object> parseHeaders(String headers, Map<String,Object> headersMap) {
        try {
            // Assuming headers are in "key=value" pairs separated by commas
            // This is a simple parsing approach; modify as per your requirements
            if (headers.contains("=")) {
                String[] pairs = headers.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        headersMap.put(keyValue[0].trim(), keyValue[1].trim());
                    }
                }
            } else {
                // If provided in JSON format, you can parse it using a JSON library like Jackson
                // For example, using Jackson:
                // headersMap = new ObjectMapper().readValue(headers, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            Logger.getLogger(RabbitMessageBuilder.class.getName()).warning("Failed to parse headers: " + e.getMessage());
        }
        return headersMap;
    }
}
