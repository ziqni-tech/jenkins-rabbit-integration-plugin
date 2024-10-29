package com.ziqni.jenkins.plugins.rabbit.utils;

import com.rabbitmq.client.AMQP;
import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * RabbitMessageBuilder class to build AMQP.BasicProperties.Builder object
 */
public abstract class RabbitMessageBuilder {

    public static AMQP.BasicProperties.Builder build(AMQP.BasicProperties.Builder builder, RabbitMessageBuilderAccessors accessors, Map<String,Object> headersMap, Function<String, String> injectEnvironmentVars) {
        if (StringUtils.isNotBlank(accessors.getContentType())) {
            builder.contentType(injectEnvironmentVars.apply(accessors.getContentType()));
        }
        if (StringUtils.isNotBlank(accessors.getContentEncoding())) {
            builder.contentEncoding(injectEnvironmentVars.apply(accessors.getContentEncoding()));
        }
        if (Objects.nonNull(accessors.getDeliveryMode())) {
            builder.deliveryMode(accessors.getDeliveryMode());
        }
        if (Objects.nonNull(accessors.getPriority())) {
            builder.priority(accessors.getPriority());
        }
        if (StringUtils.isNotBlank(accessors.getCorrelationId())) {
            builder.correlationId(injectEnvironmentVars.apply(accessors.getCorrelationId()));
        }
        if (StringUtils.isNotBlank(accessors.getReplyTo())) {
            builder.replyTo(injectEnvironmentVars.apply(accessors.getReplyTo()));
        }
        if (StringUtils.isNotBlank(accessors.getExpiration())) {
            builder.expiration(injectEnvironmentVars.apply(accessors.getExpiration()));
        }
        if (StringUtils.isNotBlank(accessors.getMessageId())) {
            builder.messageId(injectEnvironmentVars.apply(accessors.getMessageId()));
        }
        if (StringUtils.isNotBlank(accessors.getTimestamp())) {
            try {
                builder.timestamp(DateFormat.getDateInstance().parse(accessors.getTimestamp()));
            } catch (ParseException e) {
                throw new RuntimeException("Invalid timestamp format", e);
            }
        }
        if (StringUtils.isNotBlank(accessors.getType())) {
            builder.type(injectEnvironmentVars.apply(accessors.getType()));
        }
        if (StringUtils.isNotBlank(accessors.getUserId())) {
            builder.userId(injectEnvironmentVars.apply(accessors.getUserId()));
        }
        if (StringUtils.isNotBlank(accessors.getAppId())) {
            builder.appId(injectEnvironmentVars.apply(accessors.getAppId()));
        }
        if (StringUtils.isNotBlank(accessors.getClusterId())) {
            builder.clusterId(injectEnvironmentVars.apply(accessors.getClusterId()));
        }

        // Add headers if present
        if (StringUtils.isNotBlank(accessors.getHeaders())) {
            builder.headers(parseHeaders(accessors.getHeaders(), headersMap, injectEnvironmentVars));
        } else if (Objects.nonNull(headersMap)) {
            builder.headers(headersMap);
        }

        return builder;
    }

    // Method to parse headers from a JSON string or key=value format to a Map<String, Object>
    public static Map<String, Object> parseHeaders(String headers, Map<String,Object> headersMap, Function<String, String> injectEnvironmentVars) {
        try {
            // Assuming headers are in "key=value" pairs separated by commas
            // This is a simple parsing approach; modify as per your requirements
            if (headers.contains("=")) {
                String[] pairs = headers.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2 && StringUtils.isNotBlank(keyValue[0]) && StringUtils.isNotBlank(keyValue[1])) {
                        headersMap.put(keyValue[0].trim(), injectEnvironmentVars.apply(keyValue[1].trim()));
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
